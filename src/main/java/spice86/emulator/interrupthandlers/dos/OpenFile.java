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
      if (loadMemoryRange.getEndAddress() + 1 == memoryRange.getStartAddress()) {
        // fuse
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
