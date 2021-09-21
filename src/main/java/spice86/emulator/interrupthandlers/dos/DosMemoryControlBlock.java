package spice86.emulator.interrupthandlers.dos;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import spice86.emulator.memory.Memory;
import spice86.emulator.memory.MemoryUtils;
import spice86.emulator.reverseengineer.MemoryBasedDataStructureWithBaseAddress;

/**
 * Represents a MCB in memory. More info here: https://stanislavs.org/helppc/memory_control_block.html
 */
public class DosMemoryControlBlock extends MemoryBasedDataStructureWithBaseAddress {
  private static final int MCB_NON_LAST_ENTRY = 0x4D;
  private static final int MCB_LAST_ENTRY = 0x5A;
  private static final int FREE_MCB_MARKER = 0x0;

  private static final int TYPE_FIELD_OFFSET = 0;
  private static final int PSP_SEGMENT_FIELD_OFFSET = TYPE_FIELD_OFFSET + 1;
  private static final int SIZE_FIELD_OFFSET = PSP_SEGMENT_FIELD_OFFSET + 2;
  private static final int FILENAME_FIELD_OFFSET = SIZE_FIELD_OFFSET + 2 + 3;
  private static final int FILENAME_FIELD_SIZE = 8;

  public DosMemoryControlBlock(Memory memory, int baseAddress) {
    super(memory, baseAddress);
  }

  public boolean isValid() {
    return isLast() || isNonLast();
  }

  public boolean isLast() {
    return this.getType() == MCB_LAST_ENTRY;
  }

  public void setLast() {
    this.setType(MCB_LAST_ENTRY);
  }

  public boolean isNonLast() {
    return this.getType() == MCB_NON_LAST_ENTRY;
  }

  public void setNonLast() {
    this.setType(MCB_NON_LAST_ENTRY);
  }

  public boolean isFree() {
    return this.getPspSegment() == FREE_MCB_MARKER;
  }

  public void setFree() {
    this.setPspSegment(FREE_MCB_MARKER);
    this.setFileName("");
  }

  public DosMemoryControlBlock next() {
    return new DosMemoryControlBlock(this.getMemory(),
        this.getBaseAddress() + MemoryUtils.toPhysicalAddress(this.getSize() + 1, 0));
  }

  public int getUseableSpaceSegment() {
    return MemoryUtils.toSegment(this.getBaseAddress()) + 1;
  }

  public int getType() {
    return this.getUint8(TYPE_FIELD_OFFSET);
  }

  public void setType(int value) {
    this.setUint8(TYPE_FIELD_OFFSET, value);
  }

  public int getPspSegment() {
    return this.getUint16(PSP_SEGMENT_FIELD_OFFSET);
  }

  public void setPspSegment(int value) {
    this.setUint16(PSP_SEGMENT_FIELD_OFFSET, value);
  }

  // Size is in paragraph (as are segments)
  public int getSize() {
    return this.getUint16(SIZE_FIELD_OFFSET);
  }

  public void setSize(int value) {
    this.setUint16(SIZE_FIELD_OFFSET, value);
  }

  public String getFileName() {
    return super.getZeroTerminatedString(FILENAME_FIELD_OFFSET, FILENAME_FIELD_SIZE);
  }

  public void setFileName(String fileName) {
    super.setZeroTerminatedString(FILENAME_FIELD_OFFSET, fileName, FILENAME_FIELD_SIZE);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.JSON_STYLE)
        .append("type", this.getType())
        .append("pspSegment", this.getPspSegment())
        .append("size", this.getSize())
        .append("fileName", this.getFileName())
        .toString();
  }
}
