package spice86.emulator.reverseengineer;

import spice86.emulator.memory.Memory;

public class Uint16Array extends MemoryBasedArray {

  public Uint16Array(Memory memory, int baseAddress, int length) {
    super(memory, baseAddress, length);
  }

  @Override
  public int getValueSize() {
    return 2;
  }

  @Override
  public int getValueAt(int index) {
    int offset = this.indexToOffset(index);
    return getUint16(offset);
  }

  @Override
  public void setValueAt(int index, int value) {
    int offset = this.indexToOffset(index);
    setUint16(offset, value);
  }
}
