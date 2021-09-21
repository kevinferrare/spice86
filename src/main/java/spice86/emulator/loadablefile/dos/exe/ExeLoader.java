package spice86.emulator.loadablefile.dos.exe;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.cpu.State;
import spice86.emulator.loadablefile.ExecutableFileLoader;
import spice86.emulator.loadablefile.dos.PspGenerator;
import spice86.emulator.machine.Machine;
import spice86.emulator.memory.MemoryUtils;
import spice86.emulator.memory.SegmentedAddress;

/**
 * Loads a DOS 16 bits EXE file in memory.
 */
public class ExeLoader extends ExecutableFileLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExeLoader.class);
  private int startSegment;

  public ExeLoader(Machine machine, int startSegment) {
    super(machine);
    this.startSegment = startSegment;
  }

  @Override
  public byte[] loadFile(String file) throws IOException {
    byte[] exe = this.readFile(file);
    LOGGER.debug("Exe size: {}", exe.length);
    ExeFile exeFile = new ExeFile(exe);
    LOGGER.debug("Read header: {}", exeFile);
    loadExeFileInMemory(exeFile, startSegment);
    int pspSegment = startSegment - 0x10;
    setupCpuForExe(exeFile, startSegment, pspSegment);
    new PspGenerator(machine).generatePsp(pspSegment);
    LOGGER.debug("Initial CPU State: {}", cpu.getState());
    return exe;
  }

  private void loadExeFileInMemory(ExeFile exeFile, int startSegment) {
    int physicalStartAddress = MemoryUtils.toPhysicalAddress(startSegment, 0);
    memory.loadData(physicalStartAddress, exeFile.getProgramImage());
    for (SegmentedAddress address : exeFile.getRelocationTable()) {
      // Read value from memory, add the start segment offset and write back
      int addressToEdit = MemoryUtils.toPhysicalAddress(address.getSegment(), address.getOffset())
          + physicalStartAddress;
      int segmentToRelocate = memory.getUint16(addressToEdit);
      segmentToRelocate += startSegment;
      memory.setUint16(addressToEdit, segmentToRelocate);
    }
  }

  private void setupCpuForExe(ExeFile exeFile, int startSegment, int pspSegment) {
    State state = cpu.getState();
    // MS-DOS uses the values in the file header to set the SP and SS registers and
    // adjusts the initial value of the SS register by adding the start-segment
    // address to it.
    state.setSS(exeFile.getInitSS() + startSegment);
    state.setSP(exeFile.getInitSP());

    // Make DS and ES point to the PSP
    state.setDS(pspSegment);
    state.setES(pspSegment);
    // Finally, MS-DOS reads the initial CS and IP values from the program's file
    // header, adjusts the CS register value by adding the start-segment address to
    // it, and transfers control to the program at the adjusted address.
    setEntryPoint(exeFile.getInitCS() + startSegment, exeFile.getInitIP());
  }
}
