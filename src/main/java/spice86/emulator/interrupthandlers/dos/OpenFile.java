package spice86.emulator.interrupthandlers.dos;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import spice86.emulator.memory.MemoryRange;

/**
 * Represents a file opened by DOS.
 */
public class OpenFile {
  private String name;
  private int descriptor;
  private List<MemoryRange> loadMemoryRanges = new ArrayList<>();
  private RandomAccessFile randomAccessFile;

  public OpenFile(String name, int descriptor, RandomAccessFile randomAccessFile) {
    this.name = name;
    this.descriptor = descriptor;
    this.randomAccessFile = randomAccessFile;
  }

  public void addMemoryRange(MemoryRange memoryRange) {
    for (MemoryRange loadMemoryRange : loadMemoryRanges) {
      if (loadMemoryRange.getStartAddress() == memoryRange.getStartAddress()
          && loadMemoryRange.getEndAddress() == memoryRange.getEndAddress()) {
        // Same, nothing to do
        return;
      }
      if(loadMemoryRange.isInRange(memoryRange.getStartAddress(), memoryRange.getEndAddress())) {
        // Fuse
        loadMemoryRange.setStartAddress(Math.min(loadMemoryRange.getStartAddress(), memoryRange.getStartAddress()));
        loadMemoryRange.setEndAddress(Math.max(loadMemoryRange.getEndAddress(), memoryRange.getEndAddress()));
        return;
      }
      if (loadMemoryRange.getEndAddress() + 1 == memoryRange.getStartAddress()) {
        // We are the next block, extend
        loadMemoryRange.setEndAddress(memoryRange.getEndAddress());
        return;
      }
    }
    loadMemoryRanges.add(memoryRange);
  }

  public String getName() {
    return name;
  }

  public int getDescriptor() {
    return descriptor;
  }

  public List<MemoryRange> getLoadMemoryRanges() {
    return loadMemoryRanges;
  }

  public RandomAccessFile getRandomAccessFile() {
    return randomAccessFile;
  }

}
