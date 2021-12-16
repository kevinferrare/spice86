package spice86.emulator.function;

/**
 * Describes the size of an operand (8/16/32bit)
 */
public enum OperandSize {
  BYTE8(8, "byte8"), WORD16(16, "word16"), DWORD32(32, "dword32"), DWORD32PTR(32, "dword32Ptr");

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
