package spice86.emulator.cpu;

import spice86.emulator.errors.InvalidOperationException;
import spice86.emulator.machine.Machine;
import spice86.utils.ConvertUtils;

/**
 * Thrown when the operation represented by the mod r/m register index is unsupported
 */
public class InvalidGroupIndexException extends InvalidOperationException {
  public InvalidGroupIndexException(Machine machine, int groupIndex) {
    super(machine, "Invalid group index " + ConvertUtils.toHex(groupIndex));
  }
}