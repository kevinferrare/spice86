package spice86.emulator.reverseengineer;

import spice86.emulator.memory.Memory;

/**
 * Array in emulated memory
 */
public class MemoryBasedArray extends MemoryBasedDataStructureWithBaseAddress {
  private int length;

  public MemoryBasedArray(Memory memory, int baseAddress, int length) {
    super(memory, baseAddress);
    this.length = length;
  }

  public int getLength() {
    return length;
  }
}
