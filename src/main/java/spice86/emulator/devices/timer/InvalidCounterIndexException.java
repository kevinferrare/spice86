package spice86.emulator.devices.timer;

import spice86.emulator.errors.InvalidOperationException;
import spice86.emulator.machine.Machine;

/**
 * Signals that the counter index read by the code is not valid.
 *
 */
public class InvalidCounterIndexException extends InvalidOperationException {
  public InvalidCounterIndexException(Machine machine, int counterIndex) {
    super(machine, "Invalid counter index " + counterIndex);
  }
}
