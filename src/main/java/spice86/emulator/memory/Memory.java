package spice86.emulator.memory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import spice86.emulator.errors.UnrecoverableException;
import spice86.emulator.machine.breakpoint.BreakPoint;
import spice86.emulator.machine.breakpoint.BreakPointHolder;

/**
 * Addressable memory of the machine.
 */
public class Memory {
  private byte[] physicalMemory;
  private BreakPointHolder readBreakPoints = new BreakPointHolder();
  private BreakPointHolder writeBreakPoints = new BreakPointHolder();

  public Memory(int size) {
    this.physicalMemory = new byte[size];
  }

  public int getSize() {
    return physicalMemory.length;
  }

  public byte[] getRam() {
    return physicalMemory;
  }

  public void toggleBreakPoint(BreakPoint breakPoint, boolean on) {
    switch (breakPoint.getBreakPointType()) {
      case READ -> readBreakPoints.toggleBreakPoint(breakPoint, on);
      case WRITE -> writeBreakPoints.toggleBreakPoint(breakPoint, on);
      case ACCESS -> {
        readBreakPoints.toggleBreakPoint(breakPoint, on);
        writeBreakPoints.toggleBreakPoint(breakPoint, on);
      }
      default -> throw new UnrecoverableException(
          "Trying to add unsupported breakpoint of type " + breakPoint.getBreakPointType());
    }
  }

  public void loadData(int address, byte[] data) {
    loadData(address, data, data.length);
  }

  public void loadData(int address, byte[] data, int length) {
    monitorRangeWriteAccess(address, address + length);
    System.arraycopy(data, 0, physicalMemory, address, length);
  }

  public byte[] getData(int address, int length) {
    byte[] res = new byte[length];
    System.arraycopy(physicalMemory, address, res, 0, length);
    return res;
  }

  public void memCopy(int sourceAddress, int destinationAddress, int length) {
    for (int i = 0; i < length; i++) {
      int value = this.getUint8(sourceAddress + i);
      this.setUint8(destinationAddress + i, value);
    }
  }

  public void memset(int address, int value, int length) {
    for (int i = 0; i < length; i++) {
      this.setUint8(address + i, value);
    }
  }

  public int getUint8(int addr) {
    int res = MemoryUtils.getUint8(physicalMemory, addr);
    monitorReadAccess(addr);
    return res;
  }

  public void setUint8(int address, int value) {
    monitorWriteAccess(address);
    MemoryUtils.setUint8(physicalMemory, address, value);
  }

  public int getUint16(int address) {
    int res = MemoryUtils.getUint16(physicalMemory, address);
    monitorReadAccess(address);
    return res;
  }

  public void setUint16(int address, int value) {
    monitorWriteAccess(address);
    MemoryUtils.setUint16(physicalMemory, address, value);
  }

  public int getUint32(int address) {
    int res = MemoryUtils.getUint32(physicalMemory, address);
    monitorReadAccess(address);
    return res;
  }

  public void setUint32(int address, int value) {
    monitorWriteAccess(address);
    // For convenience, no get as 16 bit apps are not supposed call this directly
    MemoryUtils.setUint32(physicalMemory, address, value);
  }

  public Integer searchValue(int address, int len, List<Byte> value) {
    int end = address + len;
    if (end >= physicalMemory.length) {
      end = physicalMemory.length;
    }
    for (int i = address; i < end; i++) {
      int endValue = value.size();
      if (endValue + i >= physicalMemory.length) {
        endValue = physicalMemory.length - i;
      }
      int j = 0;
      while (j < endValue && physicalMemory[i + j] == value.get(j)) {
        j++;
      }
      if (j == endValue) {
        return i;
      }
    }
    return null;
  }

  public void dumpToFile(String path) throws IOException {
    Files.write(new File(path).toPath(), physicalMemory);
  }

  private void monitorReadAccess(int address) {
    readBreakPoints.triggerMatchingBreakPoints(address);
  }

  private void monitorWriteAccess(int address) {
    writeBreakPoints.triggerMatchingBreakPoints(address);
  }

  private void monitorRangeWriteAccess(int startAddress, int endAddress) {
    writeBreakPoints.triggerBreakPointsWithAddressRange(startAddress, endAddress);
  }
}
