package spice86.emulator.cpu;

import static spice86.utils.ConvertUtils.int16;
import static spice86.utils.ConvertUtils.int8;
import static spice86.utils.ConvertUtils.uint16;
import static spice86.utils.ConvertUtils.uint32;
import static spice86.utils.ConvertUtils.uint8;

/**
 * Implementation for the arithmetic operations performed by the CPU.
 */
public class Alu {
  /**
   * Shifting this by the number we want to test gives 1 if number of bit is even and 0 if odd.<br/>
   * Hardcoded numbers:<br/>
   * 0 -> 0000: even -> 1<br/>
   * 1 -> 0001: 1 bit so odd -> 0<br/>
   * 2 -> 0010: 1 bit so odd -> 0<br/>
   * 3 -> 0011: 2 bit so even -> 1<br/>
   * 4 -> 0100: 1 bit so odd -> 0<br/>
   * 5 -> 0101: even -> 1<br/>
   * 6 -> 0110: even -> 1<br/>
   * 7 -> 0111: odd -> 0<br/>
   * 8 -> 1000: odd -> 0<br/>
   * 9 -> 1001: even -> 1<br/>
   * A -> 1010: even -> 1<br/>
   * B -> 1011: odd -> 0<br/>
   * C -> 1100: even -> 1<br/>
   * D -> 1101: odd -> 0<br/>
   * E -> 1110: odd -> 0<br/>
   * F -> 1111: even -> 1<br/>
   * => lookup table is 1001011001101001
   */
  private static final int FOUR_BIT_PARITY_EVEN_TABLE = 0b1001011001101001;
  private static final int SHIFT_COUNT_MASK = 0x1F;
  private static final int MSB_MASK_8 = 0x80;
  private static final int BEFORE_MSB_MASK_8 = 0x40;
  private static final int MSB_MASK_16 = 0x8000;
  private static final int BEFORE_MSB_MASK_16 = 0x4000;

  private State state;

  public Alu(State state) {
    this.state = state;
  }

  // from https://www.vogons.org/viewtopic.php?t=55377
  private int overflowBitsAdd(int value1, int value2, int dst) {
    return ((value1 ^ dst) & (~(value1 ^ value2)));
  }

  private int carryBitsAdd(int value1, int value2, int dst) {
    return (((value1 ^ value2) ^ dst) ^ ((value1 ^ dst) & (~(value1 ^ value2))));
  }

  private int overflowBitsSub(int value1, int value2, int dst) {
    return ((value1 ^ dst) & (value1 ^ value2));
  }

  private int borrowBitsSub(int value1, int value2, int dst) {
    return (((value1 ^ value2) ^ dst) ^ ((value1 ^ dst) & (value1 ^ value2)));
  }

  public int add8(int value1, int value2, boolean useCarry) {
    int carry = (useCarry && state.getCarryFlag()) ? 1 : 0;
    int res = uint8(value1 + value2 + carry);
    updateFlags8(res);
    int carryBits = carryBitsAdd(value1, value2, res);
    int overflowBits = overflowBitsAdd(value1, value2, res);
    state.setCarryFlag(((carryBits >> 7) & 1) == 1);
    state.setAuxiliaryFlag(((carryBits >> 3) & 1) == 1);
    state.setOverflowFlag(((overflowBits >> 7) & 1) == 1);
    return res;
  }

  public int add16(int value1, int value2, boolean useCarry) {
    int carry = (useCarry && state.getCarryFlag()) ? 1 : 0;
    int res = uint16(value1 + value2 + carry);
    updateFlags16(res);
    int carryBits = carryBitsAdd(value1, value2, res);
    int overflowBits = overflowBitsAdd(value1, value2, res);
    state.setCarryFlag(((carryBits >> 15) & 1) == 1);
    state.setAuxiliaryFlag(((carryBits >> 3) & 1) == 1);
    state.setOverflowFlag(((overflowBits >> 15) & 1) == 1);
    return res;
  }

  public int add8(int value1, int value2) {
    return add8(value1, value2, false);
  }

  public int add16(int value1, int value2) {
    return add16(value1, value2, false);
  }

  public int adc8(int value1, int value2) {
    return add8(value1, value2, true);
  }

  public int adc16(int value1, int value2) {
    return add16(value1, value2, true);
  }

  public int inc8(int value) {
    // CF is not modified
    boolean carry = state.getCarryFlag();
    int res = add8(value, 1, false);
    state.setCarryFlag(carry);
    return res;
  }

  public int inc16(int value) {
    // CF is not modified
    boolean carry = state.getCarryFlag();
    int res = add16(value, 1, false);
    state.setCarryFlag(carry);
    return res;
  }

  public int or8(int value1, int value2) {
    int res = value1 | value2;
    updateFlags8(res);
    state.setCarryFlag(false);
    state.setOverflowFlag(false);
    return res;
  }

  public int or16(int value1, int value2) {
    int res = value1 | value2;
    updateFlags16(res);
    state.setCarryFlag(false);
    state.setOverflowFlag(false);
    return res;
  }

  public int sub8(int value1, int value2, boolean useCarry) {
    int carry = (useCarry && state.getCarryFlag()) ? 1 : 0;
    int res = uint8(value1 - value2 - carry);
    updateFlags8(res);
    int borrowBits = borrowBitsSub(value1, value2, res);
    int overflowBits = overflowBitsSub(value1, value2, res);
    state.setCarryFlag(((borrowBits >> 7) & 1) == 1);
    state.setAuxiliaryFlag(((borrowBits >> 3) & 1) == 1);
    state.setOverflowFlag(((overflowBits >> 7) & 1) == 1);

    return res;
  }

  public int sub16(int value1, int value2, boolean useCarry) {
    int carry = (useCarry && state.getCarryFlag()) ? 1 : 0;
    int res = uint16(value1 - value2 - carry);
    updateFlags16(res);
    int borrowBits = borrowBitsSub(value1, value2, res);
    int overflowBits = overflowBitsSub(value1, value2, res);
    state.setCarryFlag(((borrowBits >> 15) & 1) == 1);
    state.setAuxiliaryFlag(((borrowBits >> 3) & 1) == 1);
    state.setOverflowFlag(((overflowBits >> 15) & 1) == 1);

    return res;
  }

  public int sub8(int value1, int value2) {
    return sub8(value1, value2, false);
  }

  public int sub16(int value1, int value2) {
    return sub16(value1, value2, false);
  }

  public int sbb8(int value1, int value2) {
    return sub8(value1, value2, true);
  }

  public int sbb16(int value1, int value2) {
    return sub16(value1, value2, true);
  }

  public int dec8(int value1) {
    // CF is not modified
    boolean carry = state.getCarryFlag();
    int res = sub8(value1, 1, false);
    state.setCarryFlag(carry);
    return res;
  }

  public int dec16(int value1) {
    // CF is not modified
    boolean carry = state.getCarryFlag();
    int res = sub16(value1, 1, false);
    state.setCarryFlag(carry);
    return res;
  }

  public int and8(int value1, int value2) {
    int res = value1 & value2;
    updateFlags8(res);
    state.setCarryFlag(false);
    state.setOverflowFlag(false);
    return res;
  }

  public int and16(int value1, int value2) {
    int res = value1 & value2;
    updateFlags16(res);
    state.setCarryFlag(false);
    state.setOverflowFlag(false);
    return res;
  }

  public int xor8(int value1, int value2) {
    int res = value1 ^ value2;
    updateFlags8(res);
    state.setCarryFlag(false);
    state.setOverflowFlag(false);
    return res;
  }

  public int xor16(int value1, int value2) {
    int res = value1 ^ value2;
    updateFlags16(res);
    state.setCarryFlag(false);
    state.setOverflowFlag(false);
    return res;
  }

  public int mul8(int value1, int value2) {
    int res = value1 * value2;
    boolean upperHalfNonZero = (res & 0xFF00) != 0;
    state.setOverflowFlag(upperHalfNonZero);
    state.setCarryFlag(upperHalfNonZero);
    setZeroFlag(res);
    setParityFlag(res);
    setSignFlag8(res);
    return res;
  }

  public int mul16(int value1, int value2) {
    int res = value1 * value2;
    boolean upperHalfNonZero = (res & 0xFFFF0000) != 0;
    state.setOverflowFlag(upperHalfNonZero);
    state.setCarryFlag(upperHalfNonZero);
    setZeroFlag(res);
    setParityFlag(res);
    setSignFlag16(res);
    return res;
  }

  public int imul8(int value1, int value2) {
    int res = int8(value1) * int8(value2);
    boolean doesNotFitInByte = res != int8(res);
    state.setOverflowFlag(doesNotFitInByte);
    state.setCarryFlag(doesNotFitInByte);
    return res;
  }

  public int imul16(int value1, int value2) {
    int res = int16(value1) * int16(value2);
    boolean doesNotFitInWord = res != int16(res);
    state.setOverflowFlag(doesNotFitInWord);
    state.setCarryFlag(doesNotFitInWord);
    return res;
  }

  public Integer div8(int value1, int value2) {
    if (value2 == 0) {
      return null;
    }
    int res = value1 / value2;
    if (res > 0xFF) {
      return null;
    }
    return res;
  }

  public Integer div16(int value1, int value2) {
    if (value2 == 0) {
      return null;
    }
    long res = (uint32(value1) / value2);
    if (res > 0xFFFF) {
      return null;
    }
    return (int)res;
  }

  public Integer idiv8(int value1, int value2) {
    if (value2 == 0) {
      return null;
    }
    int res = int16(value1) / int8(value2);
    if ((res > 0x7F) || (res < (byte)0x80)) {
      return null;
    }
    return res;
  }

  public Integer idiv16(int value1, int value2) {
    if (value2 == 0) {
      return null;
    }
    int res = value1 / int16(value2);
    if ((res > 0x7FFF) || (res < (short)0x8000)) {
      return null;
    }
    return res;
  }

  public int shl8(int value, int count) {
    count &= SHIFT_COUNT_MASK;
    if (count == 0) {
      return value;
    }
    int msbBefore = (value << (count - 1)) & MSB_MASK_8;
    state.setCarryFlag(msbBefore != 0);
    int res = value << count;
    res = uint8(res);
    updateFlags8(res);
    int msb = res & MSB_MASK_8;
    state.setOverflowFlag((msb ^ msbBefore) != 0);
    return res;
  }

  public int shl16(int value, int count) {
    count &= SHIFT_COUNT_MASK;
    if (count == 0) {
      return value;
    }
    int msbBefore = (value << (count - 1)) & MSB_MASK_16;
    state.setCarryFlag(msbBefore != 0);
    int res = value << count;
    res = uint16(res);
    updateFlags16(res);
    int msb = res & MSB_MASK_16;
    state.setOverflowFlag((msb ^ msbBefore) != 0);
    return res;
  }

  public int shr8(int value, int count) {
    count &= SHIFT_COUNT_MASK;
    if (count == 0) {
      return value;
    }
    int msb = value & MSB_MASK_8;
    state.setOverflowFlag(msb != 0);
    setCarryFlagForRightShifts(value, count);
    int res = value >>> count;
    res = uint8(res);
    updateFlags8(res);
    return res;
  }

  public int shr16(int value, int count) {
    count &= SHIFT_COUNT_MASK;
    if (count == 0) {
      return value;
    }
    int msb = value & MSB_MASK_16;
    state.setOverflowFlag(msb != 0);
    setCarryFlagForRightShifts(value, count);
    int res = value >>> count;
    res = uint16(res);
    updateFlags16(res);
    return res;
  }

  public int sar8(int value, int count) {
    count &= SHIFT_COUNT_MASK;
    if (count == 0) {
      return value;
    }
    int res = int8(value);
    setCarryFlagForRightShifts(res, count);
    res >>= count;
    res = uint8(res);
    updateFlags8(res);
    state.setOverflowFlag(false);
    return res;
  }

  public int sar16(int value, int count) {
    count &= SHIFT_COUNT_MASK;
    if (count == 0) {
      return value;
    }
    int res = int16(value);
    setCarryFlagForRightShifts(res, count);
    res >>= count;
    res = uint16(res);
    updateFlags16(res);
    state.setOverflowFlag(false);
    return res;
  }

  private void setCarryFlagForRightShifts(int value, int count) {
    int lastBit = (value >>> (count - 1)) & 0x1;
    state.setCarryFlag(lastBit == 1);
  }

  public int rcl8(int value, int count) {
    count = (count & SHIFT_COUNT_MASK) % 9;
    if (count == 0) {
      return value;
    }
    int carry = (value >>> (8 - count)) & 0x1;
    int res = (value << count);
    int mask = (1 << (count - 1)) - 1;
    res |= (value >>> (9 - count)) & mask;
    if (state.getCarryFlag()) {
      res |= 1 << (count - 1);
    }
    res = uint8(res);
    state.setCarryFlag(carry != 0);

    boolean msb = (res & MSB_MASK_8) != 0;
    state.setOverflowFlag(msb ^ state.getCarryFlag());

    return res;
  }

  public int rcl16(int value, int count) {
    count = (count & SHIFT_COUNT_MASK) % 17;
    if (count == 0) {
      return value;
    }
    int carry = (value >>> (16 - count)) & 0x1;
    int res = (value << count);
    int mask = (1 << (count - 1)) - 1;
    res |= (value >>> (17 - count)) & mask;
    if (state.getCarryFlag()) {
      res |= 1 << (count - 1);
    }
    res = uint16(res);
    state.setCarryFlag(carry != 0);

    boolean msb = (res & MSB_MASK_16) != 0;
    state.setOverflowFlag(msb ^ state.getCarryFlag());
    return res;
  }

  public int rcr8(int value, int count) {
    count = (count & SHIFT_COUNT_MASK) % 9;
    if (count == 0) {
      return value;
    }
    int carry = (value >>> (count - 1)) & 0x1;
    int mask = (1 << (8 - count)) - 1;
    int res = (value >>> count) & mask;
    res |= (value << (9 - count));
    if (state.getCarryFlag()) {
      res |= 1 << (8 - count);
    }
    res = uint8(res);
    state.setCarryFlag(carry != 0);

    setOverflowForRigthRotate8(res);
    return res;
  }

  public int rcr16(int value, int count) {
    count = (count & SHIFT_COUNT_MASK) % 17;
    if (count == 0) {
      return value;
    }
    int carry = (value >>> (count - 1)) & 0x1;
    int mask = (1 << (16 - count)) - 1;
    int res = (value >>> count) & mask;
    res |= (value << (17 - count));
    if (state.getCarryFlag()) {
      res |= 1 << (16 - count);
    }
    res = uint16(res);
    state.setCarryFlag(carry != 0);

    setOverflowForRigthRotate16(res);
    return res;
  }

  public int rol8(int value, int count) {
    count = (count & SHIFT_COUNT_MASK) % 8;
    if (count == 0) {
      return value;
    }
    int carry = (value >>> (8 - count)) & 0x1;
    int res = (value << count);
    res |= (value >>> (8 - count));
    res = uint8(res);
    state.setCarryFlag(carry != 0);
    boolean msb = (res & MSB_MASK_8) != 0;
    state.setOverflowFlag(msb ^ state.getCarryFlag());
    return res;
  }

  public int rol16(int value, int count) {
    count = (count & SHIFT_COUNT_MASK) % 16;
    if (count == 0) {
      return value;
    }
    int carry = (value >>> (16 - count)) & 0x1;
    int res = (value << count);
    res |= (value >>> (16 - count));
    res = uint16(res);
    state.setCarryFlag(carry != 0);
    boolean msb = (res & MSB_MASK_16) != 0;
    state.setOverflowFlag(msb ^ state.getCarryFlag());
    return res;
  }

  public int ror8(int value, int count) {
    count = (count & SHIFT_COUNT_MASK) % 8;
    if (count == 0) {
      return value;
    }
    int carry = (value >>> (count - 1)) & 0x1;
    int mask = (1 << (8 - count)) - 1;
    int res = (value >>> count) & mask;
    res |= (value << (8 - count));
    res = uint8(res);
    state.setCarryFlag(carry != 0);

    setOverflowForRigthRotate8(res);
    return res;
  }

  public int ror16(int value, int count) {
    count = (count & SHIFT_COUNT_MASK) % 16;
    if (count == 0) {
      return value;
    }
    int carry = (value >>> (count - 1)) & 0x1;
    int mask = (1 << (16 - count)) - 1;
    int res = (value >>> count) & mask;
    res |= (value << (16 - count));
    res = uint16(res);
    state.setCarryFlag(carry != 0);

    setOverflowForRigthRotate16(res);
    return res;
  }

  private void setOverflowForRigthRotate8(int res) {
    boolean msb = (res & MSB_MASK_8) != 0;
    boolean beforeMsb = (res & BEFORE_MSB_MASK_8) != 0;
    state.setOverflowFlag(msb ^ beforeMsb);
  }

  private void setOverflowForRigthRotate16(int res) {
    boolean msb = (res & MSB_MASK_16) != 0;
    boolean beforeMsb = (res & BEFORE_MSB_MASK_16) != 0;
    state.setOverflowFlag(msb ^ beforeMsb);
  }

  public void updateFlags16(int value) {
    setZeroFlag(value);
    setParityFlag(value);
    setSignFlag16(value);
  }

  public void updateFlags8(int value) {
    setZeroFlag(value);
    setParityFlag(value);
    setSignFlag8(value);
  }

  private void setZeroFlag(int value) {
    state.setZeroFlag(value == 0);
  }

  private void setSignFlag8(int value) {
    state.setSignFlag((value & MSB_MASK_8) != 0);
  }

  private void setSignFlag16(int value) {
    state.setSignFlag((value & MSB_MASK_16) != 0);
  }

  private void setParityFlag(int value) {
    state.setParityFlag(isParity(uint8(value)));
  }

  private boolean isParity(int value) {
    int low4 = value & 0xF;
    int high4 = (value >>> 4) & 0xF;
    return ((FOUR_BIT_PARITY_EVEN_TABLE >>> low4) & 1) == ((FOUR_BIT_PARITY_EVEN_TABLE >>> high4) & 1);
  }
}
