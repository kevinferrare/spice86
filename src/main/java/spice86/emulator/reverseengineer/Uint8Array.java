package spice86.emulator.reverseengineer;

import spice86.emulator.memory.Memory;

public class Uint8Array extends MemoryBasedArray {

  public Uint8Array(Memory memory, int baseAddress, int length) {
    super(memory, baseAddress, length);
  }

  @Override
  public int getValueSize() {
    return 1;
  }

  @Override
  public int getValueAt(int index) {
    int offset = this.indexToOffset(index);
    return getUint8(offset);
  }

  @Override
  public void setValueAt(int index, int value) {
    int offset = this.indexToOffset(index);
    setUint8(offset, value);
  }
}
