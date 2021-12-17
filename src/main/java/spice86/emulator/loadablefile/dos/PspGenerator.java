package spice86.emulator.loadablefile.dos;

import org.apache.commons.lang3.StringUtils;
import spice86.emulator.interrupthandlers.dos.DosInt21Handler;
import spice86.emulator.machine.Machine;
import spice86.emulator.memory.Memory;
import spice86.emulator.memory.MemoryMap;
import spice86.emulator.memory.MemoryUtils;
import spice86.utils.ConvertUtils;

public class PspGenerator {
  private static final int LAST_FREE_SEGMENT_OFFSET = 0x02;
  private static final int DTA_OR_COMMAND_LINE_OFFSET = 0x80;
  private Machine machine;

  public PspGenerator(Machine machine) {
    this.machine = machine;
  }

  public void generatePsp(int pspSegment, String arguments) {
    Memory memory = machine.getMemory();
    int pspAddress = MemoryUtils.toPhysicalAddress(pspSegment, 0);
    // https://en.wikipedia.org/wiki/Program_Segment_Prefix
    memory.setUint16(pspAddress, 0xCD20); // INT20h
    // last free segment, dosbox seems to put it just before VRAM.
    int lastFreeSegment = MemoryMap.GRAPHIC_VIDEO_MEMORY_SEGMENT - 1;
    memory.setUint16(pspAddress + LAST_FREE_SEGMENT_OFFSET, lastFreeSegment);

    memory.loadData(pspAddress + DTA_OR_COMMAND_LINE_OFFSET, argumentsToDosBytes(arguments));

    DosInt21Handler dosFunctionDispatcher = machine.getDosInt21Handler();
    dosFunctionDispatcher.getDosMemoryManager().init(pspSegment, lastFreeSegment);
    dosFunctionDispatcher.getDosFileManager().setDiskTransferAreaAddress(pspSegment, DTA_OR_COMMAND_LINE_OFFSET);
  }

  private byte[] argumentsToDosBytes(String arguments) {
    byte[] res = new byte[128];
    String correctLengthArguments = "";
    if (StringUtils.isNotEmpty(arguments)) {
      // Cut strings longer than 127 chrs
      correctLengthArguments = arguments.length() > 127 ? arguments.substring(0, 127) : arguments;
    }
    // Command line size
    res[0] = ConvertUtils.uint8b(correctLengthArguments.length());
    // Copy actual characters
    int index = 0;
    for (; index < correctLengthArguments.length(); index++) {
      byte chr = (byte)correctLengthArguments.charAt(index);
      res[index + 1] = chr;
    }
    res[index + 1] = 0x0D;
    return res;
  }
}
