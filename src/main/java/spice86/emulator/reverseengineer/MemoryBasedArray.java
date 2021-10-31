package spice86.emulator.reverseengineer;

import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

import spice86.emulator.memory.Memory;

/**
 * Array in emulated memory
 */
public abstract class MemoryBasedArray extends MemoryBasedDataStructureWithBaseAddress {
  private int length;

  protected MemoryBasedArray(Memory memory, int baseAddress, int length) {
    super(memory, baseAddress);
    this.length = length;
  }

  public abstract int getValueSize();

  public abstract int getValueAt(int index);

  public abstract void setValueAt(int index, int value);

  public int indexToOffset(int index) {
    return index * getValueSize();
  }

  public int getLength() {
    return length;
  }

  public void forEach(IntUnaryOperator action) {
    for (int i = 0; i < length; i++) {
      int value = getValueAt(i);
      int newValue = action.applyAsInt(value);
      setValueAt(i, newValue);
    }
  }

  public void forEach(IntBinaryOperator action) {
    for (int i = 0; i < length; i++) {
      int value = getValueAt(i);
      int newValue = action.applyAsInt(i, value);
      setValueAt(i, newValue);
    }
  }
}
