package spice86.emulator.interrupthandlers.bios;

import spice86.emulator.errors.UnhandledOperationException;
import spice86.emulator.interrupthandlers.InterruptHandler;
import spice86.emulator.machine.Machine;

/**
 * Very basic implementation of int 11 that basically does nothing.
 */
public class BiosEquipmentDeterminationInt11Handler extends InterruptHandler {

  public BiosEquipmentDeterminationInt11Handler(Machine machine) {
    super(machine);
  }

  @Override
  public void run() throws UnhandledOperationException {
    state.setAX(0);
  }

  @Override
  public int getIndex() {
    return 0x11;
  }

}
