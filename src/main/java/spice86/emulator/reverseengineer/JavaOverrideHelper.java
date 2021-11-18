package spice86.emulator.reverseengineer;

import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.callback.CallbackHandler;
import spice86.emulator.cpu.Cpu;
import spice86.emulator.cpu.Stack;
import spice86.emulator.cpu.State;
import spice86.emulator.errors.InvalidOperationException;
import spice86.emulator.errors.UnrecoverableException;
import spice86.emulator.function.FunctionInformation;
import spice86.emulator.function.StaticAddressesRecorder;
import spice86.emulator.machine.Machine;
import spice86.emulator.machine.breakpoint.BreakPoint;
import spice86.emulator.machine.breakpoint.BreakPointType;
import spice86.emulator.memory.Memory;
import spice86.emulator.memory.MemoryUtils;
import spice86.emulator.memory.SegmentedAddress;
import spice86.utils.CheckedSupplier;
import spice86.utils.ConvertUtils;

/**
 * Base class to help re-implement assembly code in java.
 */
public class JavaOverrideHelper {
  private static final Logger LOGGER = LoggerFactory.getLogger(JavaOverrideHelper.class);

  private Map<SegmentedAddress, FunctionInformation> functionInformations;
  private String prefix;
  protected Machine machine;
  protected State state;
  protected Cpu cpu;
  protected Memory memory;
  protected Stack stack;

  public JavaOverrideHelper(Map<SegmentedAddress, FunctionInformation> functionInformations, String prefix,
      Machine machine) {
    // Getting the global map to be able to do duplication checks
    this.functionInformations = functionInformations;
    this.prefix = prefix;
    this.machine = machine;
    this.cpu = machine.getCpu();
    this.memory = machine.getMemory();
    this.state = cpu.getState();
    this.stack = cpu.getStack();
  }

  /**
   * Define an override calling the actual callback on each registered callbacks.<br/>
   * This is to mark as overridable functions that only call provided interrupts
   */
  public void setProvidedInterruptHandlersAsOverridden() {
    CallbackHandler callbackHandler = machine.getCallbackHandler();
    Map<Integer, SegmentedAddress> callbackAddresses = callbackHandler.getCallbackAddresses();
    for (Map.Entry<Integer, SegmentedAddress> callbackAddressEnty : callbackAddresses.entrySet()) {
      int callbackNumber = callbackAddressEnty.getKey();
      SegmentedAddress callbackAddress = callbackAddressEnty.getValue();
      defineFunction(callbackAddress.getSegment(), callbackAddress.getOffset(),
          "provided_interrupt_handler_" + ConvertUtils.toHex(callbackNumber), () -> {
            callbackHandler.run(callbackNumber);
            return interruptRet();
          });
    }
  }

  public Runnable nearRet() {
    return () -> cpu.nearRet(0);
  }

  public Runnable farRet() {
    return () -> cpu.farRet(0);
  }

  public Runnable interruptRet() {
    return () -> cpu.interruptRet();
  }

  public Runnable nearJump(int ip) {
    return () -> state.setIP(ip);
  }

  public Runnable farJump(int cs, int ip) {
    return () -> {
      state.setCS(cs);
      state.setIP(ip);
    };
  }

  public void defineFunction(int segment, int offset, String suffix) {
    this.defineFunction(segment, offset, suffix, null);
  }

  public void defineFunction(int segment, int offset, String suffix,
      CheckedSupplier<Runnable, InvalidOperationException> override) {
    SegmentedAddress address = new SegmentedAddress(segment, offset);
    FunctionInformation existingFunctionInformation = functionInformations.get(address);
    String name = prefix + "." + suffix;
    if (existingFunctionInformation != null) {
      String error =
          "There is already a function defined at address " + address + " named "
              + existingFunctionInformation.getName() + " but you are trying to redefine it as " + name
              + ". Please check your mappings for duplicates.";
      LOGGER.error(error);
      throw new UnrecoverableException(error);
    }
    FunctionInformation functionInformation = new FunctionInformation(address, name, override);
    functionInformations.put(address, functionInformation);
  }

  public void defineStaticAddress(int segment, int offset, String name) {
    defineStaticAddress(segment, offset, name, false);
  }
  public void defineStaticAddress(int segment, int offset, String name, boolean whiteListOnlyThisSegment) {
    SegmentedAddress address = new SegmentedAddress(segment, offset);
    int physicalAddress = address.toPhysical();
    StaticAddressesRecorder recorder = cpu.getStaticAddressesRecorder();
    String existing = recorder.getNames().get(physicalAddress);
    if (existing != null) {
      
      String error =
          "There is already a static address defined at address " + address + " named "
              + existing + " but you are trying to redefine it as " + name
              + ". Please check your mappings for duplicates.";
      LOGGER.error(error);
      throw new UnrecoverableException(error);
    }
    recorder.addName(physicalAddress, name);
    if(whiteListOnlyThisSegment) {
      recorder.addSegmentTowhiteList(address);
    }
  }

  /**
   * Calls override instead of the instruction at segment:offset.<br/>
   * Warning: your implementation need to return a Runnable that will set back the address for the next instruction to
   * be executed.
   * 
   * @param segment
   * @param offset
   * @param override
   */
  public void overrideInstruction(int segment, int offset, Supplier<Runnable> override) {
    // Implementing this via the breakpoint mechanism already in place
    BreakPoint breakPoint =
        new BreakPoint(BreakPointType.EXECUTION, MemoryUtils.toPhysicalAddress(segment, offset),
            b -> override.get().run(),
            false);
    machine.getMachineBreakpoints().toggleBreakPoint(breakPoint, true);
  }

  /**
   * Sanity check. Checks that the value at the given address points the the address we expect.
   * 
   * @param segmentRegisterIndex
   * @param offset
   * @param expectedSegment
   * @param expectedOffset
   */
  protected void checkVtableContainsExpected(int segmentRegisterIndex, int offset, int expectedSegment,
      int expectedOffset) {
    int address = MemoryUtils.toPhysicalAddress(state.getSegmentRegisters().getRegister(segmentRegisterIndex), offset);
    int foundOffset = memory.getUint16(address);
    int foundSegment = memory.getUint16(address + 2);
    if (foundOffset != expectedOffset || foundSegment != expectedSegment) {
      this.failAsUntested("Call table value changed, we would not call the method the game is calling. Expected: "
          + new SegmentedAddress(expectedSegment, expectedOffset) + " found: "
          + new SegmentedAddress(foundSegment, foundOffset));
    }
  }

  /**
   * Call this in your override when you re-implement a function with a branch that seems never reached.
   * 
   * @param message
   */
  protected void failAsUntested(String message) {
    String error = """
        Untested code reached, please tell us how to reach this state.
        Here is the message:
        """
        + message +
        """

            Here is the call stack:
            """
        + this.machine.dumpCallStack();

    LOGGER.error(error);
    throw new UnrecoverableException(error);
  }
}
