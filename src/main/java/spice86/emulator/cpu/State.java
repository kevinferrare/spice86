package spice86.emulator.cpu;

import static spice86.utils.ConvertUtils.toHex16;
import static spice86.utils.ConvertUtils.uint16;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import spice86.emulator.memory.MemoryUtils;
import spice86.utils.ConvertUtils;

/**
 * State of the CPU:
 * <ul>
 * <li>Registers</li>
 * <li>Segment registers</li>
 * <li>Flags</li>
 * </ul>
 * Provides some easy methods to get and set registers by their names
 */
public class State {
  // CPU state
  private Registers registers = new Registers();
  private SegmentRegisters segmentRegisters = new SegmentRegisters();
  // Instruction pointer, always points to a valid instruction, cpu uses another value mid-decode.
  private int ip;
  private Flags flags = new Flags();
  private long cycles;

  // In-instruction state
  private Integer segmentOverrideIndex = null;
  private Boolean continueZeroFlagValue = null;

  // CPU log
  private String currentInstructionPrefix = "";
  private String currentInstructionName = "";

  public Registers getRegisters() {
    return registers;
  }

  public SegmentRegisters getSegmentRegisters() {
    return segmentRegisters;
  }

  public Flags getFlags() {
    return flags;
  }

  // AX
  public int getAX() {
    return registers.getRegister(Registers.AX_INDEX);
  }

  public void setAX(int value) {
    registers.setRegister(Registers.AX_INDEX, value);
  }

  public int getAL() {
    return registers.getRegister8L(Registers.AX_INDEX);
  }

  public void setAL(int value) {
    registers.setRegister8L(Registers.AX_INDEX, value);
  }

  public int getAH() {
    return registers.getRegister8H(Registers.AX_INDEX);
  }

  public void setAH(int value) {
    registers.setRegister8H(Registers.AX_INDEX, value);
  }

  // CX
  public int getCX() {
    return registers.getRegister(Registers.CX_INDEX);
  }

  public void setCX(int value) {
    registers.setRegister(Registers.CX_INDEX, value);
  }

  public int getCL() {
    return registers.getRegister8L(Registers.CX_INDEX);
  }

  public void setCL(int value) {
    registers.setRegister8L(Registers.CX_INDEX, value);
  }

  public int getCH() {
    return registers.getRegister8H(Registers.CX_INDEX);
  }

  public void setCH(int value) {
    registers.setRegister8H(Registers.CX_INDEX, value);
  }

  // DX
  public int getDX() {
    return registers.getRegister(Registers.DX_INDEX);
  }

  public void setDX(int value) {
    registers.setRegister(Registers.DX_INDEX, value);
  }

  public int getDL() {
    return registers.getRegister8L(Registers.DX_INDEX);
  }

  public void setDL(int value) {
    registers.setRegister8L(Registers.DX_INDEX, value);
  }

  public int getDH() {
    return registers.getRegister8H(Registers.DX_INDEX);
  }

  public void setDH(int value) {
    registers.setRegister8H(Registers.DX_INDEX, value);
  }

  // BX
  public int getBX() {
    return registers.getRegister(Registers.BX_INDEX);
  }

  public void setBX(int value) {
    registers.setRegister(Registers.BX_INDEX, value);
  }

  public int getBL() {
    return registers.getRegister8L(Registers.BX_INDEX);
  }

  public void setBL(int value) {
    registers.setRegister8L(Registers.BX_INDEX, value);
  }

  public int getBH() {
    return registers.getRegister8H(Registers.BX_INDEX);
  }

  public void setBH(int value) {
    registers.setRegister8H(Registers.BX_INDEX, value);
  }

  public int getSP() {
    return registers.getRegister(Registers.SP_INDEX);
  }

  public void setSP(int value) {
    registers.setRegister(Registers.SP_INDEX, value);
  }

  public int getBP() {
    return registers.getRegister(Registers.BP_INDEX);
  }

  public void setBP(int value) {
    registers.setRegister(Registers.BP_INDEX, value);
  }

  public int getSI() {
    return registers.getRegister(Registers.SI_INDEX);
  }

  public void setSI(int value) {
    registers.setRegister(Registers.SI_INDEX, value);
  }

  public int getDI() {
    return registers.getRegister(Registers.DI_INDEX);
  }

  public void setDI(int value) {
    registers.setRegister(Registers.DI_INDEX, value);
  }

  public int getES() {
    return segmentRegisters.getRegister(SegmentRegisters.ES_INDEX);
  }

  public void setES(int value) {
    segmentRegisters.setRegister(SegmentRegisters.ES_INDEX, value);
  }

  public int getCS() {
    return segmentRegisters.getRegister(SegmentRegisters.CS_INDEX);
  }

  public void setCS(int value) {
    segmentRegisters.setRegister(SegmentRegisters.CS_INDEX, value);
  }

  public int getSS() {
    return segmentRegisters.getRegister(SegmentRegisters.SS_INDEX);
  }

  public void setSS(int value) {
    segmentRegisters.setRegister(SegmentRegisters.SS_INDEX, value);
  }

  public int getDS() {
    return segmentRegisters.getRegister(SegmentRegisters.DS_INDEX);
  }

  public void setDS(int value) {
    segmentRegisters.setRegister(SegmentRegisters.DS_INDEX, value);
  }

  public int getFS() {
    return segmentRegisters.getRegister(SegmentRegisters.FS_INDEX);
  }

  public void setFS(int value) {
    segmentRegisters.setRegister(SegmentRegisters.FS_INDEX, value);
  }

  public int getGS() {
    return segmentRegisters.getRegister(SegmentRegisters.GS_INDEX);
  }

  public void setGS(int value) {
    segmentRegisters.setRegister(SegmentRegisters.GS_INDEX, value);
  }

  public int getIP() {
    return ip;
  }

  public void setIP(int value) {
    ip = uint16(value);
  }

  public boolean getCarryFlag() {
    return flags.getFlag(Flags.CARRY);
  }

  public boolean getParityFlag() {
    return flags.getFlag(Flags.PARITY);
  }

  public boolean getAuxiliaryFlag() {
    return flags.getFlag(Flags.AUXILIARY);
  }

  public boolean getZeroFlag() {
    return flags.getFlag(Flags.ZERO);
  }

  public boolean getSignFlag() {
    return flags.getFlag(Flags.SIGN);
  }

  public boolean getTrapFlag() {
    return flags.getFlag(Flags.TRAP);
  }

  public boolean getInterruptFlag() {
    return flags.getFlag(Flags.INTERRUPT);
  }

  public boolean getDirectionFlag() {
    return flags.getFlag(Flags.DIRECTION);
  }

  public boolean getOverflowFlag() {
    return flags.getFlag(Flags.OVERFLOW);
  }

  public void setCarryFlag(boolean value) {
    flags.setFlag(Flags.CARRY, value);
  }

  public void setParityFlag(boolean value) {
    flags.setFlag(Flags.PARITY, value);
  }

  public void setAuxiliaryFlag(boolean value) {
    flags.setFlag(Flags.AUXILIARY, value);
  }

  public void setZeroFlag(boolean value) {
    flags.setFlag(Flags.ZERO, value);
  }

  public void setSignFlag(boolean value) {
    flags.setFlag(Flags.SIGN, value);
  }

  public void setTrapFlag(boolean value) {
    flags.setFlag(Flags.TRAP, value);
  }

  public void setInterruptFlag(boolean value) {
    flags.setFlag(Flags.INTERRUPT, value);
  }

  public void setDirectionFlag(boolean value) {
    flags.setFlag(Flags.DIRECTION, value);
  }

  public void setOverflowFlag(boolean value) {
    flags.setFlag(Flags.OVERFLOW, value);
  }

  public Integer getSegmentOverrideIndex() {
    return segmentOverrideIndex;
  }

  public void setSegmentOverrideIndex(Integer segmentOverrideIndex) {
    this.segmentOverrideIndex = segmentOverrideIndex;
  }

  public Boolean getContinueZeroFlagValue() {
    return continueZeroFlagValue;
  }

  public void setContinueZeroFlagValue(Boolean continueZeroFlagValue) {
    this.continueZeroFlagValue = continueZeroFlagValue;
  }

  public void clearPrefixes() {
    this.setContinueZeroFlagValue(null);
    this.setSegmentOverrideIndex(null);
  }

  public int getStackPhysicalAddress() {
    return MemoryUtils.toPhysicalAddress(this.getSS(), this.getSP());
  }

  public int getIpPhysicalAddress() {
    return MemoryUtils.toPhysicalAddress(this.getCS(), this.getIP());
  }

  public void resetCurrentInstructionPrefix() {
    this.currentInstructionPrefix = "";
  }

  public void addCurrentInstructionPrefix(String currentInstructionPrefix) {
    this.currentInstructionPrefix += currentInstructionPrefix + " ";
  }

  public String getCurrentInstructionNameWithPrefix() {
    return currentInstructionPrefix + currentInstructionName;
  }

  public void setCurrentInstructionName(String currentInstructionName) {
    this.currentInstructionName = currentInstructionName;
  }

  public long getCycles() {
    return cycles;
  }

  public void setCycles(long cycles) {
    this.cycles = cycles;
  }

  public void incCycles() {
    cycles++;
  }

  public String dumpRegFlags() {
    String res = "cycles=" + this.getCycles();
    res += " CS:IP=" + ConvertUtils.toSegmentedAddressRepresentation(getCS(), getIP()) + '/'
        + ConvertUtils.toAbsoluteSegmentedAddress(getCS(), getIP());
    res += " AX=" + toHex16(getAX());
    res += " BX=" + toHex16(getBX());
    res += " CX=" + toHex16(getCX());
    res += " DX=" + toHex16(getDX());
    res += " SI=" + toHex16(getSI());
    res += " DI=" + toHex16(getDI());
    res += " BP=" + toHex16(getBP());
    res += " SP=" + toHex16(getSP());
    res += " SS=" + toHex16(getSS());
    res += " DS=" + toHex16(getDS());
    res += " ES=" + toHex16(getES());
    res += " FS=" + toHex16(getFS());
    res += " GS=" + toHex16(getGS());
    res += " flags=" + toHex16(flags.getFlagRegister());
    res += " (" + flags.toString() + ")";
    return res;
  }

  @Override
  public String toString() {
    return dumpRegFlags();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(ip).append(flags).append(registers).append(segmentRegisters).hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    return (obj instanceof State other)
        && this.ip == other.ip
        && this.flags.equals(other.flags)
        && this.registers.equals(other.registers)
        && this.segmentRegisters.equals(other.segmentRegisters);
  }
}
