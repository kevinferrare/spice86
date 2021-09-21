package spice86.emulator.memory;

import com.google.gson.Gson;

/**
 * Represents a range in memory.
 */
public class MemoryRange {
  private int startAddress;
  private int endAddress;
  private String name;

  public MemoryRange(int startAddress, int endAddress, String name) {
    this.startAddress = startAddress;
    this.endAddress = endAddress;
    this.name = name;
  }

  public int getStartAddress() {
    return startAddress;
  }

  public void setStartAddress(int startAddress) {
    this.startAddress = startAddress;
  }

  public int getEndAddress() {
    return endAddress;
  }

  public void setEndAddress(int endAddress) {
    this.endAddress = endAddress;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isInRange(int rangeStartAddress, int rangeEndAddress) {
    return rangeStartAddress <= endAddress && rangeEndAddress >= startAddress;
  }

  public boolean isInRange(int address) {
    return startAddress <= address && address <= endAddress;
  }

  public static MemoryRange fromSegment(int segmentStart, int length, String name) {
    return fromSegment(segmentStart, 0, length, name);
  }

  public static MemoryRange fromSegment(int segment, int startOffset, int length, String name) {
    int start = MemoryUtils.toPhysicalAddress(segment, startOffset);
    int end = MemoryUtils.toPhysicalAddress(segment, startOffset + length);
    return new MemoryRange(start, end, name);
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }
}
