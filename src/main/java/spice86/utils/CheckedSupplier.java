package spice86.utils;

/**
 * Like java.util.function.Supplier but can throw a checked exception.
 */
@FunctionalInterface
public interface CheckedSupplier<T, E extends Throwable> {
  T get() throws E;
}
