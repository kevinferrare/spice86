package spice86.emulator.reverseengineer;

import spice86.emulator.memory.Memory;

/**
 * Data structure that uses emulated memory for storage.<br/>
 * Allows to store and retrieve data from a baseAddress and an offset.<br/>
 * Base class for structured storage of user provided overrides.<br/>
 * Allows them to be able to define structures in sync with the emulated memory.
 */
public class MemoryBasedDataStructure {
  private Memory memory;

  public MemoryBasedDataStructure(Memory memory) {
    this.memory = memory;
  }

  public Memory getMemory() {
    return memory;
  }

  public int getUint8(int baseAddress, int offset) {
    return memory.getUint8(baseAddress + offset);
  }

  public void setUint8(int baseAddress, int offset, int value) {
    memory.setUint8(baseAddress + offset, value);
  }

  public int getUint16(int baseAddress, int offset) {
    return memory.getUint16(baseAddress + offset);
  }

  public void setUint16(int baseAddress, int offset, int value) {
    memory.setUint16(baseAddress + offset, value);
  }

  public Uint8Array getUint8Array(int baseAddress, int start, int length) {
    return new Uint8Array(memory, baseAddress + start, length);
  }

  public Uint16Array getUint16Array(int baseAddress, int start, int length) {
    return new Uint16Array(memory, baseAddress + start, length);
  }
}
