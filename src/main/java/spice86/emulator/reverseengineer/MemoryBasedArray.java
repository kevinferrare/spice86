package spice86.emulator.reverseengineer;

import java.util.function.BiFunction;
import java.util.function.Function;

import spice86.emulator.memory.Memory;

/**
 * Array in emulated memory
 */
public abstract class MemoryBasedArray extends MemoryBasedDataStructureWithBaseAddress {
  private int length;

  public MemoryBasedArray(Memory memory, int baseAddress, int length) {
    super(memory, baseAddress);
    this.length = length;
  }

  abstract public int getValueSize();

  abstract public int getValueAt(int index);

  abstract public void setValueAt(int index, int value);

  public int indexToOffset(int index) {
    return index * getValueSize();
  }

  public int getLength() {
    return length;
  }

  public void forEach(Function<Integer, Integer> action) {
    for (int i = 0; i < length; i++) {
      int value = getValueAt(i);
      int newValue = action.apply(value);
      setValueAt(i, newValue);
    }
  }

  public void forEach(BiFunction<Integer, Integer, Integer> action) {
    for (int i = 0; i < length; i++) {
      int value = getValueAt(i);
      int newValue = action.apply(i, value);
      setValueAt(i, newValue);
    }
  }
}
