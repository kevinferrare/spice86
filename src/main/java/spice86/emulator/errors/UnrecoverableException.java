package spice86.emulator.errors;

/**
 * Unchecked exception used in cases when the program is in a situation when recovery is not possible. 
 *
 */
public class UnrecoverableException extends RuntimeException {

  public UnrecoverableException(String message, Throwable cause) {
    super(message, cause);
  }

  public UnrecoverableException(String message) {
    super(message);
  }

  public UnrecoverableException(Throwable cause) {
    super(cause);
  }

}
