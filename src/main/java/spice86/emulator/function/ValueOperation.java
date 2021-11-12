package spice86.emulator.function;

/**
 * Describes possible operations done to an address in memory (read / write)
 */
public enum ValueOperation {
  READ, WRITE;

  public ValueOperation oppositeOperation() {
    if (this == READ) {
      return WRITE;
    }
    return READ;
  }
}
