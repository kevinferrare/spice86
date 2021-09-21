package spice86.emulator.reverseengineer;

import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.cpu.Cpu;
import spice86.emulator.cpu.State;
import spice86.emulator.errors.UnrecoverableException;
import spice86.emulator.function.FunctionInformation;
import spice86.emulator.machine.Machine;
import spice86.emulator.machine.breakpoint.BreakPoint;
import spice86.emulator.machine.breakpoint.BreakPointType;
import spice86.emulator.memory.Memory;
import spice86.emulator.memory.MemoryUtils;
import spice86.emulator.memory.SegmentedAddress;

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

  public JavaOverrideHelper(Map<SegmentedAddress, FunctionInformation> functionInformations, String prefix,
      Machine machine) {
    // Getting the global map to be able to do duplication checks
    this.functionInformations = functionInformations;
    this.prefix = prefix;
    this.machine = machine;
    this.cpu = machine.getCpu();
    this.memory = machine.getMemory();
    this.state = cpu.getState();
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

  public void defineFunction(int segment, int offset, String suffix, Supplier<Runnable> override) {
    SegmentedAddress address = new SegmentedAddress(segment, offset);
    FunctionInformation existingFunctionInformation = functionInformations.get(address);
    String name = prefix + "." + suffix;
    if (existingFunctionInformation != null) {
      String error =
          "There is already a function defined at address " + address + " named "
              + existingFunctionInformation.getName() + " but you are trying to redefine it with as " + name
              + ". Please check your mappings for duplicates.";
      LOGGER.error(error);
      throw new UnrecoverableException(error);
    }
    FunctionInformation functionInformation = new FunctionInformation(address, name, override);
    functionInformations.put(address, functionInformation);
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
