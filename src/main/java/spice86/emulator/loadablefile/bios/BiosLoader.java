package spice86.emulator.loadablefile.bios;

import java.io.IOException;

import spice86.emulator.loadablefile.ExecutableFileLoader;
import spice86.emulator.machine.Machine;
import spice86.emulator.memory.MemoryUtils;

/**
 * Loader for bios files.<br/>
 * Bios entry point is at physical address 0xFFFF0 (F000:FFF0).
 */
public class BiosLoader extends ExecutableFileLoader {
  private static final int CODE_OFFSET = 0xFFF0;
  private static final int CODE_SEGMENT = 0xF000;

  public BiosLoader(Machine machine) {
    super(machine);
  }

  @Override
  public byte[] loadFile(String file, String arguments) throws IOException {
    byte[] bios = this.readFile(file);
    int physicalStartAddress = MemoryUtils.toPhysicalAddress(CODE_SEGMENT, 0);
    memory.loadData(physicalStartAddress, bios);
    this.setEntryPoint(CODE_SEGMENT, CODE_OFFSET);
    return bios;
  }
}
