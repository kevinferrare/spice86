package spice86.emulator.interrupthandlers;

import spice86.emulator.errors.UnhandledOperationException;
import spice86.emulator.machine.Machine;

/**
 * Signals that the operation for the given callback is not handled.
 */
public class UnhandledInterruptException extends UnhandledOperationException {
  public UnhandledInterruptException(Machine machine, int callbackNumber, int operation) {
    super(machine, formatMessage(callbackNumber, operation));
  }

  private static String formatMessage(int callbackNumber, int operation) {
    return String.format("callbackNumber=0x%X, operation=0x%X", callbackNumber, operation);
  }
}
