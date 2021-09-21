package spice86.emulator.callback;

import spice86.emulator.errors.UnhandledOperationException;
import spice86.utils.CheckedRunnable;

/**
 * Base interface for a callback.
 */
public interface Callback extends CheckedRunnable<UnhandledOperationException> {
  /**
   * @return the index of the callback. For interrupts it would be the interrupt number.
   */
  public int getIndex();
}
