package spice86.emulator.cpu;

import static spice86.utils.ConvertUtils.uint16;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import spice86.utils.ConvertUtils;

/**
 * Holder for a group of registers. Handles various operations like:
 * <ul>
 * <li>Accessing a register via its index</li>
 * <li>Accessing registers low and high 8 bit parts.</li>
 * <li>Getting the register name from its index</li>
 * </ul>
 */
public class RegistersHolder {
  // Registers allowing access to their high / low parts have indexes from 0 to 3 so 2 bits
  private static final int REGISTER8_INDEX_HIGH_LOW_MASK = 0b11;
  // 3rd bit in register index means to access the high part
  private static final int REGISTER8_INDEX_HIGH_BIT_MASK = 0b100;

  private Map<Integer, String> registersNames;
  private int[] registers;

  public RegistersHolder(Map<Integer, String> registersNames) {
    this.registersNames = registersNames;
    this.registers = new int[registersNames.size()];
  }

  public String getRegName(int regIndex) {
    return registersNames.get(regIndex);
  }

  public String getReg8Name(int regIndex) {
    String suffix = ((regIndex & REGISTER8_INDEX_HIGH_BIT_MASK) == 1) ? "H" : "L";
    String reg16 = getRegName(regIndex & REGISTER8_INDEX_HIGH_LOW_MASK);
    return StringUtils.substring(reg16, 0, 1) + suffix;
  }

  public int getRegister(int index) {
    return registers[index];
  }

  public void setRegister(int index, int value) {
    registers[index] = uint16(value);
  }

  public int getRegister8L(int regIndex) {
    return ConvertUtils.readLsb(getRegister(regIndex));
  }

  public void setRegister8L(int regIndex, int value) {
    int currentValue = getRegister(regIndex);
    int newValue = ConvertUtils.writeLsb(currentValue, value);
    setRegister(regIndex, newValue);
  }

  public int getRegister8H(int regIndex) {
    return ConvertUtils.readMsb(getRegister(regIndex));
  }

  public void setRegister8H(int regIndex, int value) {
    int currentValue = getRegister(regIndex);
    int newValue = ConvertUtils.writeMsb(currentValue, value);
    setRegister(regIndex, newValue);
  }

  public int getRegisterFromHighLowIndex8(int index) {
    int indexInArray = index & REGISTER8_INDEX_HIGH_LOW_MASK;
    if ((index & REGISTER8_INDEX_HIGH_BIT_MASK) != 0) {
      return getRegister8H(indexInArray);
    }
    return getRegister8L(indexInArray);
  }

  public void setRegisterFromHighLowIndex8(int index, int value) {
    int indexInArray = index & REGISTER8_INDEX_HIGH_LOW_MASK;
    if ((index & REGISTER8_INDEX_HIGH_BIT_MASK) != 0) {
      setRegister8H(indexInArray, value);
    } else {
      setRegister8L(indexInArray, value);
    }
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(registers).hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    return (obj instanceof RegistersHolder other) && Arrays.equals(this.registers, other.registers);
  }
}
