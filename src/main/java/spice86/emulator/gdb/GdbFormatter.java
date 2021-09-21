package spice86.emulator.gdb;

import static spice86.utils.ConvertUtils.swap32;

import spice86.utils.ConvertUtils;

public class GdbFormatter {

  public String formatValueAsHex8(int value) {
    return String.format("%02X", ConvertUtils.uint8(value));
  }

  public String formatValueAsHex32(int value) {
    // Convert to little endian
    return String.format("%08X", swap32(value));
  }
}
