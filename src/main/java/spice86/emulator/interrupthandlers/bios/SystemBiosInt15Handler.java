package spice86.emulator.interrupthandlers.bios;

import spice86.emulator.errors.UnhandledOperationException;
import spice86.emulator.interrupthandlers.InterruptHandler;
import spice86.emulator.machine.Machine;

public class SystemBiosInt15Handler extends InterruptHandler {

  public SystemBiosInt15Handler(Machine machine) {
    super(machine);
    this.dispatchTable.put(0xC0, this::unsupported);
  }

  @Override
  public void run() throws UnhandledOperationException {
    int operation = state.getAH();
    this.run(operation);
  }

  @Override
  public int getIndex() {
    return 0x15;
  }

  private void unsupported() {
    // We are not an IBM PS/2
    super.setCarryFlag(true, true);
    state.setAH(0x86);
  }
}
