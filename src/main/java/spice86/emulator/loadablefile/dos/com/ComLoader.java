package spice86.emulator.loadablefile.dos.com;

import java.io.IOException;

import spice86.emulator.cpu.State;
import spice86.emulator.loadablefile.ExecutableFileLoader;
import spice86.emulator.loadablefile.dos.PspGenerator;
import spice86.emulator.machine.Machine;
import spice86.emulator.memory.MemoryUtils;

public class ComLoader extends ExecutableFileLoader {
  private static final int COM_OFFSET = 0x100;
  private int startSegment;

  public ComLoader(Machine machine, int startSegment) {
    super(machine);
    this.startSegment = startSegment;
  }

  @Override
  public byte[] loadFile(String file) throws IOException {
    new PspGenerator(machine).generatePsp(startSegment);

    byte[] com = this.readFile(file);
    int physicalStartAddress = MemoryUtils.toPhysicalAddress(startSegment, COM_OFFSET);
    memory.loadData(physicalStartAddress, com);

    State state = cpu.getState();
    // Make DS and ES point to the PSP
    state.setDS(startSegment);
    state.setES(startSegment);
    setEntryPoint(startSegment, COM_OFFSET);
    return com;
  }
}
