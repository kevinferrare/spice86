package spice86.emulator.errors;

import spice86.emulator.machine.Machine;

/**
 * Thrown when an unsupported / invalid operation is requested.
 */
public class UnhandledOperationException extends InvalidOperationException {
  public UnhandledOperationException(Machine machine, String message) {
    super(machine, message);
  }
}
