package spice86.emulator.callback;

import java.util.HashMap;
import java.util.Map;

import spice86.emulator.errors.UnhandledOperationException;
import spice86.utils.CheckedRunnable;

/**
 * Base class for most classes having to dispatch operations depending on a numeric value, like interrupts.
 */
public abstract class IndexBasedDispatcher<T extends CheckedRunnable<UnhandledOperationException>> {

  protected Map<Integer, T> dispatchTable = new HashMap<>();

  public void run(Integer index) throws UnhandledOperationException {
    T handler = dispatchTable.get(index);
    if (handler == null) {
      throw generateUnhandledOperationException(index);
    }
    handler.run();
  }

  public void addService(int index, T runnable) {
    this.dispatchTable.put(index, runnable);
  }

  protected abstract UnhandledOperationException generateUnhandledOperationException(int index);
}
