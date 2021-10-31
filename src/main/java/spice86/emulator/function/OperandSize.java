package spice86.emulator.function;

/**
 * Describes the size of an operand (8/16/32bit)
 */
public enum OperandSize {
  BYTE8(8, "Byte8"), WORD16(16, "Word16"), DWORD32(32, "Dwod32");

  private int bits;
  private String name;

  private OperandSize(int bits, String name) {
    this.bits = bits;
    this.name = name;
  }

  public int getBits() {
    return bits;
  }

  public String getName() {
    return name;
  }
}
