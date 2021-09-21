package spice86.emulator.function;

import java.util.Map;

import spice86.emulator.machine.Machine;
import spice86.emulator.memory.SegmentedAddress;

/**
 * Provides overrides and function names to the running program.<br/>
 * This is your entry point if you are re-implementing parts of a program.
 */
public interface OverrideSupplier {
  public Map<SegmentedAddress, FunctionInformation> generateFunctionInformations(int programStartAddress,
      Machine machine);
}
