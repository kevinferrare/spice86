package spice86.emulator.loadablefile.dos.exe;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import spice86.emulator.memory.MemoryUtils;
import spice86.emulator.memory.SegmentedAddress;

/**
 * Representation of a DOS 16 bits EXE file.<br/>
 * More details about the format here: https://bytepointer.com/resources/dos_programmers_ref_exe_format.htm
 */
public class ExeFile {
  private String signature; // 0000 - Magic number
  private int extraBytes; // 0002 - Bytes on last page of file
  private int pages; // 0004 - Pages in file
  private int relocItems; // 0006 - Relocations
  private int headerSize; // 0008 - Size of header in paragraphs
  private int minAlloc; // 000A - Minimum extra paragraphs needed
  private int maxAlloc; // 000C - Maximum extra paragraphs needed
  private int initSS; // 000E - Initial (relative) SS value
  private int initSP; // 0010 - Initial SP value
  private int checkSum; // 0012 - Checksum
  private int initIP; // 0014 - Initial IP value
  private int initCS; // 0016 - Initial (relative) CS value
  private int relocTable; // 0018 - File address of relocation table
  private int overlay; // 001A - Overlay number

  private List<SegmentedAddress> relocationTable = new ArrayList<>();

  private byte[] programImage;

  public ExeFile(byte[] exe) {
    this.signature = new String(exe, 0, 2, StandardCharsets.UTF_8);
    this.extraBytes = MemoryUtils.getUint16(exe, 0x02);
    this.pages = MemoryUtils.getUint16(exe, 0x04);
    this.relocItems = MemoryUtils.getUint16(exe, 0x06);
    this.headerSize = MemoryUtils.getUint16(exe, 0x08);
    this.minAlloc = MemoryUtils.getUint16(exe, 0x0A);
    this.maxAlloc = MemoryUtils.getUint16(exe, 0x0C);
    this.initSS = MemoryUtils.getUint16(exe, 0x0E);
    this.initSP = MemoryUtils.getUint16(exe, 0x10);
    this.checkSum = MemoryUtils.getUint16(exe, 0x12);
    this.initIP = MemoryUtils.getUint16(exe, 0x14);
    this.initCS = MemoryUtils.getUint16(exe, 0x16);
    this.relocTable = MemoryUtils.getUint16(exe, 0x18);
    this.overlay = MemoryUtils.getUint16(exe, 0x1A);

    int relocationTableOffset = this.relocTable;
    int numRelocationEntries = this.relocItems;
    for (int i = 0; i < numRelocationEntries; i++) {
      int offset = MemoryUtils.getUint16(exe, relocationTableOffset + i * 4);
      int segment = MemoryUtils.getUint16(exe, relocationTableOffset + i * 4 + 2);
      relocationTable.add(new SegmentedAddress(segment, offset));
    }
    int actualHeaderSize = headerSize * 16;
    int programSize = exe.length - actualHeaderSize;
    programImage = new byte[programSize];
    System.arraycopy(exe, actualHeaderSize, programImage, 0, programSize);
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public int getExtraBytes() {
    return extraBytes;
  }

  public void setExtraBytes(int extraBytes) {
    this.extraBytes = extraBytes;
  }

  public int getPages() {
    return pages;
  }

  public void setPages(int pages) {
    this.pages = pages;
  }

  public int getRelocItems() {
    return relocItems;
  }

  public void setRelocItems(int relocItems) {
    this.relocItems = relocItems;
  }

  public int getHeaderSize() {
    return headerSize;
  }

  public void setHeaderSize(int headerSize) {
    this.headerSize = headerSize;
  }

  public int getMinAlloc() {
    return minAlloc;
  }

  public void setMinAlloc(int minAlloc) {
    this.minAlloc = minAlloc;
  }

  public int getMaxAlloc() {
    return maxAlloc;
  }

  public void setMaxAlloc(int maxAlloc) {
    this.maxAlloc = maxAlloc;
  }

  public int getInitSS() {
    return initSS;
  }

  public void setInitSS(int initSS) {
    this.initSS = initSS;
  }

  public int getInitSP() {
    return initSP;
  }

  public void setInitSP(int initSP) {
    this.initSP = initSP;
  }

  public int getCheckSum() {
    return checkSum;
  }

  public void setCheckSum(int checkSum) {
    this.checkSum = checkSum;
  }

  public int getInitIP() {
    return initIP;
  }

  public void setInitIP(int initIP) {
    this.initIP = initIP;
  }

  public int getInitCS() {
    return initCS;
  }

  public void setInitCS(int initCS) {
    this.initCS = initCS;
  }

  public int getRelocTable() {
    return relocTable;
  }

  public void setRelocTable(int relocTable) {
    this.relocTable = relocTable;
  }

  public int getOverlay() {
    return overlay;
  }

  public void setOverlay(int overlay) {
    this.overlay = overlay;
  }

  public byte[] getProgramImage() {
    return programImage;
  }

  public void setProgramImage(byte[] programImage) {
    this.programImage = programImage;
  }

  public void setRelocationTable(List<SegmentedAddress> relocationTable) {
    this.relocationTable = relocationTable;
  }

  public List<SegmentedAddress> getRelocationTable() {
    return relocationTable;
  }

  public int getCodeSize() {
    return programImage.length;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.JSON_STYLE).toString();
  }

}