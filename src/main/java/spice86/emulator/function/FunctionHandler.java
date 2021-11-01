package spice86.emulator.function;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.cpu.Cpu;
import spice86.emulator.cpu.State;
import spice86.emulator.errors.InvalidOperationException;
import spice86.emulator.machine.Machine;
import spice86.emulator.memory.Memory;
import spice86.emulator.memory.SegmentedAddress;
import spice86.utils.ConvertUtils;

/**
 * Called whenever a call / interrupt / ret is executed.<br/>
 * Does some dynamic analysis of the program flow and executes function overrides if provided.
 */
public class FunctionHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(FunctionHandler.class);

  private Machine machine;
  private Deque<FunctionCall> callerStack = new ArrayDeque<>();
  private Map<SegmentedAddress, FunctionInformation> functionInformations = new HashMap<>();
  private boolean useCodeOverride;
  private boolean debugMode;

  public FunctionHandler(Machine machine, boolean debugMode) {
    this.machine = machine;
    this.debugMode = debugMode;
  }

  public void setFunctionInformations(Map<SegmentedAddress, FunctionInformation> functionInformations) {
    this.functionInformations = functionInformations;
  }

  public Map<SegmentedAddress, FunctionInformation> getFunctionInformations() {
    return functionInformations;
  }

  public void setUseCodeOverride(boolean useCodeOverride) {
    this.useCodeOverride = useCodeOverride;
  }

  public void icall(CallType callType, int entrySegment, int entryOffset, Integer expectedReturnSegment,
      Integer expectedReturnOffset, int vectorNumber, boolean recordReturn) throws InvalidOperationException {
    call(callType, entrySegment, entryOffset, expectedReturnSegment, expectedReturnOffset,
        () -> "interrupt_handler_" + ConvertUtils.toHex(vectorNumber), recordReturn);
  }

  public void call(CallType callType, int entrySegment, int entryOffset, Integer expectedReturnSegment,
      Integer expectedReturnOffset) throws InvalidOperationException {
    call(callType, entrySegment, entryOffset, expectedReturnSegment, expectedReturnOffset, null, true);
  }

  /**
   * Handles a function call:
   * <ul>
   * <li>Creates if necessary a FunctionInformation</li>
   * <li>Manages a callstack to be able to know the caller of the FunctionInformation being called</li>
   * <li>If the function information has an override, calls it and calls its retHandler when done (usually this would be
   * a retNear / retFar / iret) so that the stack is back to a good state for next emulation</li>
   * </ul>
   * 
   * @param callType
   *          how this function was called (near, far, interrupt, machine start)
   * @param entrySegment
   *          function entry segment
   * @param entryOffset
   *          function entry offset
   * @param expectedReturnSegment
   *          expected return segment
   * @param expectedReturnOffset
   *          expected return offset
   * @param nameGenerator
   *          Supplier for a name for newly discovered functions. Not used if null.
   * @param recordReturn
   *          if true, keep the record in a list for this call. False value would be for external interrupt for which
   *          return addresses change all the time.
   * @throws InvalidOperationException
   */
  public void call(CallType callType, int entrySegment, int entryOffset, Integer expectedReturnSegment,
      Integer expectedReturnOffset, Supplier<String> nameGenerator, boolean recordReturn)
      throws InvalidOperationException {
    SegmentedAddress entryAddress = new SegmentedAddress(entrySegment, entryOffset);
    FunctionInformation currentFunction = functionInformations.computeIfAbsent(entryAddress,
        k -> new FunctionInformation(entryAddress, nameGenerator != null ? nameGenerator.get() : "unknown"));
    if (debugMode) {
      // Determine caller
      FunctionInformation caller = getFunctionInformation(getCurrentFunctionCall());
      // Characterize current function
      SegmentedAddress expectedReturnAddress = null;
      if (expectedReturnSegment != null && expectedReturnOffset != null) {
        expectedReturnAddress = new SegmentedAddress(expectedReturnSegment, expectedReturnOffset);
      }
      FunctionCall currentFunctionCall =
          new FunctionCall(callType, entryAddress, expectedReturnAddress, getCurrentStackAddress(), recordReturn);
      callerStack.addFirst(currentFunctionCall);
      // Do the call
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Calling {} from {}", currentFunction, caller);
      }
      currentFunction.enter(caller);
    }
    if (useCodeOverride) {
      currentFunction.callOverride();
    }
  }

  public boolean ret(CallType returnCallType) {
    if (debugMode) {
      FunctionCall currentFunctionCall = callerStack.pollFirst();
      if (currentFunctionCall == null) {
        LOGGER.warn("Returning but no call was done before!!");
        return false;
      }
      FunctionInformation currentFunctionInformation = getFunctionInformation(currentFunctionCall);
      boolean returnAddressAlignedWithCallStack =
          addReturn(returnCallType, currentFunctionCall, currentFunctionInformation);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Returning from {} to {}", currentFunctionInformation,
            getFunctionInformation(getCurrentFunctionCall()));
      }

      if (!returnAddressAlignedWithCallStack) {
        // Put it back in the stack, we did a jump not a return
        callerStack.addFirst(currentFunctionCall);
      }
    }
    return true;
  }

  /**
   * Checks that the return address currently on the VM stack (targeted by SS:SP) is aligned with the expected return
   * address computed at function call time.<br/>
   * This is because some programs manipulate the stack.
   * 
   * @param returnCallType
   *          type of return to inspect the VM stack
   * @param currentFunctionCall
   *          information gathered at the time the function was called to compare with VM stack
   * @param currentFunctionInformation
   *          informations about the function referred by currentFunctionCall
   * @return true if return was as expected, false if it points somewhere else than at call time.
   */
  private boolean addReturn(CallType returnCallType, FunctionCall currentFunctionCall,
      FunctionInformation currentFunctionInformation) {
    FunctionReturn currentFunctionReturn = generateCurrentFunctionReturn(returnCallType);
    SegmentedAddress actualReturnAddress = peekReturnAddressOnMachineStack(returnCallType);

    boolean returnAddressAlignedWithCallStack =
        isReturnAddressAlignedWithCallStack(currentFunctionCall, actualReturnAddress, currentFunctionReturn);

    // Do not register returns for overrides
    if (currentFunctionInformation != null && !useOverride(currentFunctionInformation)) {
      SegmentedAddress addressToRecord = actualReturnAddress;
      if (!currentFunctionCall.isRecordReturn()) {
        // Do not record this address. This is mainly for external interrupts for which return address does not make
        // sense
        addressToRecord = null;
      }

      if (returnAddressAlignedWithCallStack) {
        currentFunctionInformation.addReturn(currentFunctionReturn, addressToRecord);
      } else {
        currentFunctionInformation.addUnalignedReturn(currentFunctionReturn, addressToRecord);
      }
    }
    return returnAddressAlignedWithCallStack;
  }

  private boolean isReturnAddressAlignedWithCallStack(FunctionCall currentFunctionCall,
      SegmentedAddress actualReturnAddress, FunctionReturn currentFunctionReturn) {
    SegmentedAddress expectedReturnAddress = currentFunctionCall.getExpectedReturnAddress();

    // Null check necessary for machine stop call, in this case it won't be equals to what is in the stack but it's
    // expected.
    if (actualReturnAddress != null && !actualReturnAddress.equals(expectedReturnAddress)) {
      FunctionInformation currentFunctionInformation = getFunctionInformation(currentFunctionCall);
      if (LOGGER.isInfoEnabled()
          && !currentFunctionInformation.getUnalignedReturns().containsKey(currentFunctionReturn)) {
        CallType callType = currentFunctionCall.getCallType();
        SegmentedAddress stackAddressAfterCall = currentFunctionCall.getStackAddressAfterCall();
        SegmentedAddress returnAddressOnCallTimeStack =
            peekReturnAddressOnMachineStack(callType, stackAddressAfterCall.toPhysical());
        SegmentedAddress currentStackAddress = getCurrentStackAddress();
        String additionalInformation = "\n";
        if (!currentStackAddress.equals(stackAddressAfterCall)) {
          int delta = Math.abs(currentStackAddress.toPhysical() - stackAddressAfterCall.toPhysical());
          additionalInformation +=
              "Stack is not pointing at the same address as it was at call time. Delta is " + delta + " bytes\n";
        }
        if (!Objects.equals(expectedReturnAddress, returnAddressOnCallTimeStack)) {
          additionalInformation += "Return address on stack was modified";
        }
        LOGGER.info("""
            PROGRAM IS NOT WELL BEHAVED SO CALL STACK COULD NOT BE TRACEABLE ANYMORE!
            Current function {} return {} will not go to the expected place:
             - At {} call time, return was supposed to be {} stored at SS:SP {}. Value there is now {}
             - On the stack it is now {} stored at SS:SP {}{}
            """,
            currentFunctionInformation, currentFunctionReturn,
            //
            callType, expectedReturnAddress, stackAddressAfterCall, returnAddressOnCallTimeStack,
            //
            actualReturnAddress, currentStackAddress,
            //
            additionalInformation);
      }
      return false;
    }
    return true;
  }

  private int getStackPhysicalAddress() {
    return machine.getCpu().getState().getStackPhysicalAddress();
  }

  private SegmentedAddress getCurrentStackAddress() {
    State state = machine.getCpu().getState();
    return new SegmentedAddress(state.getSS(), state.getSP());
  }

  public SegmentedAddress peekReturnAddressOnMachineStack(CallType returnCallType) {
    int stackPhysicalAddress = getStackPhysicalAddress();
    return peekReturnAddressOnMachineStack(returnCallType, stackPhysicalAddress);
  }

  public SegmentedAddress peekReturnAddressOnMachineStack(CallType returnCallType, int stackPhysicalAddress) {
    Memory memory = machine.getMemory();
    State state = machine.getCpu().getState();
    return switch (returnCallType) {
      case NEAR -> new SegmentedAddress(state.getCS(), memory.getUint16(stackPhysicalAddress));
      case FAR, INTERRUPT -> new SegmentedAddress(memory.getUint16(stackPhysicalAddress + 2),
          memory.getUint16(stackPhysicalAddress));
      case MACHINE -> null;
    };
  }

  public SegmentedAddress peekReturnAddressOnMachineStackForCurrentFunction() {
    FunctionCall currentFunctionCall = getCurrentFunctionCall();
    if (currentFunctionCall == null) {
      return null;
    }
    return peekReturnAddressOnMachineStack(currentFunctionCall.getCallType());
  }

  private FunctionReturn generateCurrentFunctionReturn(CallType returnCallType) {
    Cpu cpu = machine.getCpu();
    State state = cpu.getState();
    int cs = state.getCS();
    int ip = state.getIP();
    return new FunctionReturn(returnCallType, new SegmentedAddress(cs, ip));
  }

  private FunctionCall getCurrentFunctionCall() {
    if (callerStack.isEmpty()) {
      return null;
    }
    return callerStack.peekFirst();
  }

  private boolean useOverride(FunctionInformation functionInformation) {
    return this.useCodeOverride && functionInformation != null && functionInformation.hasOverride();
  }

  private FunctionInformation getFunctionInformation(FunctionCall functionCall) {
    if (functionCall == null) {
      return null;
    }
    return functionInformations.get(functionCall.getEntryPointAddress());
  }

  public String dumpCallStack() {
    StringBuilder res = new StringBuilder();
    for (FunctionCall functionCall : this.callerStack) {
      SegmentedAddress returnAddress = functionCall.getExpectedReturnAddress();
      FunctionInformation functionInformation = getFunctionInformation(functionCall);
      res.append(" - ");
      res.append(functionInformation);
      res.append(" expected to return to address ");
      res.append(returnAddress);
      res.append('\n');
    }
    return res.toString();
  }
}
