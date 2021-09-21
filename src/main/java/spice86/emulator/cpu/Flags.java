package spice86.emulator.cpu;

import spice86.utils.ConvertUtils;

/**
 * Handles the CPU flag register.
 */
public class Flags {
  // @formatter:off
  public static final int CARRY     = 0b00000000_00000001;
  public static final int PARITY    = 0b00000000_00000100;
  public static final int AUXILIARY = 0b00000000_00010000;
  public static final int ZERO      = 0b00000000_01000000;
  public static final int SIGN      = 0b00000000_10000000;
  public static final int TRAP      = 0b00000001_00000000;
  public static final int INTERRUPT = 0b00000010_00000000;
  public static final int DIRECTION = 0b00000100_00000000;
  public static final int OVERFLOW  = 0b00001000_00000000;
  // @formatter:on

  private static char getFlag(int flags, int mask, char representation) {
    if ((flags & mask) == 0) {
      return ' ';
    }
    return representation;
  }

  public static String dumpFlags(int flags) {
    String res = "";
    res += getFlag(flags, Flags.OVERFLOW, 'O');
    res += getFlag(flags, Flags.DIRECTION, 'D');
    res += getFlag(flags, Flags.INTERRUPT, 'I');
    res += getFlag(flags, Flags.TRAP, 'T');
    res += getFlag(flags, Flags.SIGN, 'S');
    res += getFlag(flags, Flags.ZERO, 'Z');
    res += getFlag(flags, Flags.AUXILIARY, 'A');
    res += getFlag(flags, Flags.PARITY, 'P');
    res += getFlag(flags, Flags.CARRY, 'C');
    return res;
  }

  // rflag mask to OR with flags, useful to compare values with dosbox which emulates
  private int additionalFlagMask;
  private int flagRegister;

  public Flags() {
    this.setFlagRegister(0);
  }

  public void setDosboxCompatibility(boolean compatible) {
    if (compatible) {
      additionalFlagMask = 0b111000000000000;
    } else {
      additionalFlagMask = 0;
    }
  }

  public int getFlagRegister() {
    return flagRegister;
  }

  public void setFlagRegister(int value) {
    // Some flags are always 1 or 0 no matter what (8086)
    int modifedValue = (value | 0b10) & 0b0111111111010111;
    // dosbox
    modifedValue |= additionalFlagMask;
    flagRegister = ConvertUtils.uint16(modifedValue);
  }

  public boolean getFlag(int mask) {
    return (flagRegister & mask) == mask;
  }

  public void setFlag(int mask, boolean value) {
    if (value) {
      flagRegister |= mask;
    } else {
      flagRegister &= ~mask;
    }
  }

  @Override
  public String toString() {
    return dumpFlags(flagRegister);
  }

  @Override
  public int hashCode() {
    return flagRegister;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    return (obj instanceof Flags other) && this.flagRegister == other.flagRegister;
  }
}