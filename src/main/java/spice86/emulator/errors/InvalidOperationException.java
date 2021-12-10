package spice86.emulator.errors;

import spice86.emulator.machine.Machine;

/**
 * Base class for exceptions occurring in the VM.<br/>
 * Gives the VM status in the generated error message.
 */
public class InvalidOperationException extends Exception {

  public InvalidOperationException(Machine machine, String message) {
    super(generateStatusMessage(machine, message));
  }

  public InvalidOperationException(Machine machine, Throwable cause) {
    super(generateStatusMessage(machine, null), cause);
  }

  protected static String generateStatusMessage(Machine machine, String message) {
    String error = "An error occurred while machine was in this state: " + machine.getCpu().getState().toString();
    if(message!=null) {
      error+=".\nError is: " + message;
    }
    return error;
  }
}
