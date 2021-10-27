package spice86.emulator.reverseengineer;

import spice86.emulator.errors.UnrecoverableException;
import spice86.emulator.memory.Memory;

/**
 * Memory based data structure where providing the base address is defined by the subclass and automatically used.
 */
public abstract class MemoryBasedDataStructureWithBaseAddressProvider extends MemoryBasedDataStructure {

  protected MemoryBasedDataStructureWithBaseAddressProvider(Memory memory) {
    super(memory);
  }

  public abstract int getBaseAddress();

  public int getUint8(int offset) {
    return super.getUint8(getBaseAddress(), offset);
  }

  public void setUint8(int offset, int value) {
    super.setUint8(getBaseAddress(), offset, value);
  }

  public int getUint16(int offset) {
    return super.getUint16(getBaseAddress(), offset);
  }

  public void setUint16(int offset, int value) {
    super.setUint16(getBaseAddress(), offset, value);
  }

  public Uint8Array getUint8Array(int start, int length) {
    return super.getUint8Array(getBaseAddress(), start, length);
  }

  public Uint16Array getUint16Array(int start, int length) {
    return super.getUint16Array(getBaseAddress(), start, length);
  }

  public String getZeroTerminatedString(int start, int maxLength) {
    StringBuilder res = new StringBuilder();
    int physicalStart = getBaseAddress() + start;
    for (int i = 0; i < maxLength; i++) {
      char character = (char)super.getUint8(physicalStart, i);
      if (character == 0) {
        break;
      }
      res.append(character);
    }
    return res.toString();
  }

  public void setZeroTerminatedString(int start, String value, int maxLenght) {
    if (value.length() + 1 > maxLenght) {
      throw new UnrecoverableException(
          "String " + value + " is more than " + maxLenght + " cannot write it at offset " + start);
    }
    int physicalStart = getBaseAddress() + start;
    int i = 0;
    for (; i < value.length(); i++) {
      char character = value.charAt(i);
      super.setUint8(physicalStart, i, character);
    }
    super.setUint8(physicalStart, i, 0);
  }
}
