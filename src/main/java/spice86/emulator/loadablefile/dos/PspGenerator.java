package spice86.emulator.loadablefile.dos;

import spice86.emulator.interrupthandlers.dos.DosInt21Handler;
import spice86.emulator.machine.Machine;
import spice86.emulator.memory.Memory;
import spice86.emulator.memory.MemoryMap;
import spice86.emulator.memory.MemoryUtils;

public class PspGenerator {
  private static final int LAST_FREE_SEGMENT_OFFSET = 0x02;
  private static final int DTA_OR_COMMAND_LINE_OFFSET = 0x80;
  private Machine machine;

  public PspGenerator(Machine machine) {
    this.machine = machine;
  }

  public void generatePsp(int pspSegment) {
    Memory memory = machine.getMemory();
    int pspAddress = MemoryUtils.toPhysicalAddress(pspSegment, 0);
    // https://en.wikipedia.org/wiki/Program_Segment_Prefix
    memory.setUint16(pspAddress, 0xCD20); // INT20h
    // last free segment, dosbox seems to put it just before VRAM.
    int lastFreeSegment = MemoryMap.GRAPHIC_VIDEO_MEMORY_SEGMENT - 1;
    memory.setUint16(pspAddress + LAST_FREE_SEGMENT_OFFSET, lastFreeSegment);

    // Command line size, 0
    memory.setUint8(pspAddress + DTA_OR_COMMAND_LINE_OFFSET, 0x00);
    // Command line ended by 0x0D, make it empty
    memory.setUint8(pspAddress + DTA_OR_COMMAND_LINE_OFFSET + 1, 0x0D);

    DosInt21Handler dosFunctionDispatcher = machine.getDosInt21Handler();
    dosFunctionDispatcher.getDosMemoryManager().init(pspSegment, lastFreeSegment);
    dosFunctionDispatcher.getDosFileManager().setDiskTransferAreaAddress(pspSegment, DTA_OR_COMMAND_LINE_OFFSET);
  }

}
