package spice86.emulator.gdb;

import static spice86.utils.ConvertUtils.swap32;

public class GdbFormatter {

  public String formatValueAsHex8(int value) {
    return String.format("%02X", value);
  }

  public String formatValueAsHex32(int value) {
    // Convert to little endian
    return String.format("%08X", swap32(value));
  }

  public String formatValueAsHex(byte[] value) {
    StringBuilder stringBuilder = new StringBuilder(value.length * 2);
    for (byte b : value) {
      stringBuilder.append(formatValueAsHex8(b));
    }
    return stringBuilder.toString();
  }

}
