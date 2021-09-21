package spice86.emulator.interrupthandlers.dos;

import spice86.emulator.memory.Memory;
import spice86.emulator.reverseengineer.MemoryBasedDataStructureWithBaseAddress;

/**
 * Represents a DTA in memory. More info here: https://stanislavs.org/helppc/dta.html
 */
public class DosDiskTransferArea extends MemoryBasedDataStructureWithBaseAddress {

  private static final int ATTRIBUTE_OFFSET = 0x15;
  private static final int FILE_TIME_OFFSET = 0x16;
  private static final int FILE_DATE_OFFSET = 0x18;
  private static final int FILE_SIZE_OFFSET = 0x1A;
  private static final int FILE_NAME_OFFSET = 0x1E;
  private static final int FILE_NAME_SIZE = 13;

  public DosDiskTransferArea(Memory memory, int baseAddress) {
    super(memory, baseAddress);
  }

  public int getAttribute() {
    return this.getUint8(ATTRIBUTE_OFFSET);
  }

  public void setAttribute(int value) {
    this.setUint8(ATTRIBUTE_OFFSET, value);
  }

  public int getFileTime() {
    return this.getUint16(FILE_TIME_OFFSET);
  }

  public void setFileTime(int value) {
    this.setUint16(FILE_TIME_OFFSET, value);
  }

  public int getFileDate() {
    return this.getUint16(FILE_DATE_OFFSET);
  }

  public void setFileDate(int value) {
    this.setUint16(FILE_DATE_OFFSET, value);
  }

  public int getFileSize() {
    return this.getUint16(FILE_SIZE_OFFSET);
  }

  public void setFileSize(int value) {
    this.setUint16(FILE_SIZE_OFFSET, value);
  }

  public String getFileName() {
    return this.getZeroTerminatedString(FILE_NAME_OFFSET, FILE_NAME_SIZE);
  }

  public void setFileName(String value) {
    this.setZeroTerminatedString(FILE_NAME_OFFSET, value, FILE_NAME_SIZE);
  }
}
