package spice86.emulator.loadablefile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.cpu.Cpu;
import spice86.emulator.cpu.State;
import spice86.emulator.machine.Machine;
import spice86.emulator.memory.Memory;
import spice86.utils.ConvertUtils;

/**
 * Base class for loading executable files in the VM like exe, bios, ...
 */
public abstract class ExecutableFileLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExecutableFileLoader.class);

  protected Machine machine;
  protected Cpu cpu;
  protected Memory memory;

  protected ExecutableFileLoader(Machine machine) {
    this.machine = machine;
    this.cpu = machine.getCpu();
    this.memory = machine.getMemory();
  }

  /**
   * 
   * @param file
   * @return the bytes read from the given file
   * @throws IOException
   */
  public abstract byte[] loadFile(String file, String arguments) throws IOException;

  protected byte[] readFile(String file) throws IOException {
    return Files.readAllBytes(Paths.get(file));
  }

  protected void setEntryPoint(int cs, int ip) {
    State state = cpu.getState();
    state.setCS(cs);
    state.setIP(ip);
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Program entry point is {}", ConvertUtils.toSegmentedAddressRepresentation(cs, ip));
    }
  }
}
