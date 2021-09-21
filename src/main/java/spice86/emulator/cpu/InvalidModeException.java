package spice86.emulator.cpu;

import spice86.emulator.errors.InvalidOperationException;
import spice86.emulator.machine.Machine;
import spice86.utils.ConvertUtils;

/**
 * Thrown when the mode parsed from mod r/m is unsupported (Should never happen?)
 */
public class InvalidModeException extends InvalidOperationException {
  public InvalidModeException(Machine machine, int mode) {
    super(machine, "Invalid mode " + ConvertUtils.toHex(mode));
  }
}