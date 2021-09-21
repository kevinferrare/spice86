package spice86.emulator.callback;

import spice86.emulator.errors.UnhandledOperationException;
import spice86.emulator.machine.Machine;

/**
 * Exception signaling that the callback number that was meant to be executed was not mapped to any java code.<br/>
 * Could happen for unhandled exceptions.
 */
public class UnhandledCallbackException extends UnhandledOperationException {
  public UnhandledCallbackException(Machine machine, int callbackNumber) {
    super(machine, formatMessage(callbackNumber));
  }

  private static String formatMessage(int callbackNumber) {
    return String.format("callbackNumber=0x%X", callbackNumber);
  }
}
