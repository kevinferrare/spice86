package spice86.utils;

import java.nio.charset.StandardCharsets;

import spice86.emulator.memory.MemoryUtils;

public class ConvertUtils {
  private static final int SEGMENT_SIZE = 0x10000;

  public static String toHex(byte value) {
    return String.format("0x%X", value);
  }

  public static String toHex(short value) {
    return String.format("0x%X", value);
  }

  public static String toHex(int value) {
    return String.format("0x%X", value);
  }

  public static String toHex8(int value) {
    return String.format("0x%X", uint8(value));
  }

  public static String toHex16(int value) {
    return String.format("0x%X", uint16(value));
  }

  public static String toBin8(int value) {
    return Integer.toBinaryString(uint8(value));
  }

  public static String toBin16(int value) {
    return Integer.toBinaryString(uint16(value));
  }

  public static char toChar(int value) {
    return new String(new byte[] { uint8b(value) }, StandardCharsets.US_ASCII).toCharArray()[0];
  }

  public static String toSegmentedAddressRepresentation(int segment, int offset) {
    return toHex16(segment) + ":" + toHex16(offset);
  }

  public static String toAbsoluteSegmentedAddress(int segment, int offset) {
    int physical = MemoryUtils.toPhysicalAddress(segment, offset);
    return toHex16(toAbsoluteSegment(physical)) + ":" + toHex16(toAbsoluteOffset(physical));
  }

  public static int toAbsoluteSegment(int physicalAddress) {
    return ((physicalAddress / SEGMENT_SIZE) * SEGMENT_SIZE) >>> 4;
  }

  public static int toAbsoluteOffset(int physicalAddress) {
    return physicalAddress - (physicalAddress / SEGMENT_SIZE) * SEGMENT_SIZE;
  }

  public static int uint8(int value) {
    return value & 0xFF;
  }

  public static int uint16(int value) {
    return value & 0xFFFF;
  }

  public static long uint32(long value) {
    return value & 0xFFFFFFFFL;
  }

  public static int uint32i(long value) {
    return (int)uint32(value);
  }

  public static byte uint8b(int value) {
    return (byte)uint8(value);
  }

  /**
   * Sign extend value considering it is a 8 bit value
   * 
   * @param value
   * @return the value sign extended
   */
  public static int int8(int value) {
    return (byte)value;
  }

  /**
   * Sign extend value considering it is a 16 bit value
   * 
   * @param value
   * @return the value sign extended
   */
  public static int int16(int value) {
    return (short)value;
  }

  public static int swap32(int value) {
    return (((value >>> 24) & 0x000000ff) |
        ((value >>> 8) & 0x0000ff00) |
        ((value << 8) & 0x00ff0000) |
        ((value << 24) & 0xff000000));
  }

  public static byte[] hexToByteArray(String string) {
    byte[] res = new byte[string.length() / 2];
    for (int i = 0; i < string.length(); i += 2) {
      String hex = string.substring(i, i + 2);
      // parsing as Integer since Byte.parseByte only supports signed bytes.
      int value = Integer.parseInt(hex, 16);
      res[i / 2] = (byte)value;
    }
    return res;
  }

  /**
   * @param value
   * @return a long since unsigned ints are not a thing in java
   */
  public static long parseHex32(String value) {
    return Long.parseLong(value, 16);
  }

  public static long bytesToInt32(byte[] data, int start) {
    return uint32(((data[start] << 24) & 0xFF000000) |
        ((data[start + 1] << 16) & 0x00FF0000) |
        ((data[start + 2] << 8) & 0x0000FF00) |
        ((data[start + 3]) & 0x000000FF));
  }
}
