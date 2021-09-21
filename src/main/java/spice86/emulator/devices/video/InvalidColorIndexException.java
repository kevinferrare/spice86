package spice86.emulator.devices.video;

import spice86.emulator.errors.InvalidOperationException;
import spice86.emulator.machine.Machine;

/**
 * Signals that the color index read by the code is not valid.
 *
 */
public class InvalidColorIndexException extends InvalidOperationException {
  public InvalidColorIndexException(Machine machine, int color) {
    super(machine, "Color index " + color + " is invalid");
  }
}
