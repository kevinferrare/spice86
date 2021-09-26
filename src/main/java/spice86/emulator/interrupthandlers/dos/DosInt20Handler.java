package spice86.emulator.interrupthandlers.dos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.errors.UnhandledOperationException;
import spice86.emulator.interrupthandlers.InterruptHandler;
import spice86.emulator.machine.Machine;
/**
 * Reimplementation of int20
 */
public class DosInt20Handler extends InterruptHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(DosInt20Handler.class);

  public DosInt20Handler(Machine machine) {
    super(machine);
  }

  @Override
  public int getIndex() {
    return 0x20;
  }

  @Override
  public void run() throws UnhandledOperationException {
    LOGGER.info("PROGRAM TERMINATE");
    cpu.setRunning(false);
  }

}
