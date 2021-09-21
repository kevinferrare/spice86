package spice86.emulator.memory;

import static spice86.utils.ConvertUtils.uint16;
import static spice86.utils.ConvertUtils.uint8;
import static spice86.utils.ConvertUtils.uint8b;

/**
 * Utils to get and set values in an array. Words and DWords are considered to be stored little-endian.<br/>
 */
public class MemoryUtils {
  public static int getUint8(byte[] memory, int address) {
    return uint8(memory[address]);
  }

  public static void setUint8(byte[] memory, int address, int value) {
    memory[address] = uint8b(value);
  }

  public static int getUint16(byte[] memory, int address) {
    return uint16(uint8(memory[address]) | (uint8(memory[address + 1]) << 8));
  }

  public static void setUint16(byte[] memory, int address, int value) {
    memory[address] = uint8b(value);
    memory[address + 1] = uint8b(value >>> 8);
  }

  public static int getUint32(byte[] memory, int address) {
    return uint8(memory[address]) | (uint8(memory[address + 1]) << 8) | (uint8(memory[address + 2]) << 16)
        | (uint8(memory[address + 3]) << 24);
  }

  public static void setUint32(byte[] memory, int address, int value) {
    memory[address] = uint8b(value);
    memory[address + 1] = uint8b(value >>> 8);
    memory[address + 2] = uint8b(value >>> 16);
    memory[address + 3] = uint8b(value >>> 24);
  }

  public static int toPhysicalAddress(int segment, int offset) {
    return (uint16(segment) << 4) + uint16(offset);
  }

  public static int toSegment(int physicalAddress) {
    return uint16(physicalAddress >>> 4);
  }
}
