package spice86.utils;

/**
 * Like java.util.function.Consumer but can throw a checked exception.
 */
@FunctionalInterface
public interface CheckedConsumer<T, E extends Throwable> {
  void accept(T t) throws E;
}
