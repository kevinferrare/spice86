package spice86.utils;

/**
 * Like java.lang.Runnable but can throw a checked exception.
 */
@FunctionalInterface
public interface CheckedRunnable<T extends Throwable> {
  void run() throws T;
}
