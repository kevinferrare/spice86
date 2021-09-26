package spice86.emulator.devices.sound;

import spice86.emulator.ioports.DefaultIOPortHandler;
import spice86.emulator.ioports.IOPortDispatcher;
import spice86.emulator.machine.Machine;

/**
 * Gravis Ultra Sound implementation. Emulates an absent card :)
 */
public class GravisUltraSound extends DefaultIOPortHandler {
  private static final int MIX_CONTROL_REGISTER = 0x240;
  private static final int READ_DATA_OR_TRIGGER_STATUS = 0x241;
  private static final int IRQ_STATUS_REGISTER = 0x246;
  private static final int TIMER_CONTROL_REGISTER = 0x248;
  private static final int IRQ_CONTROL_REGISTER = 0x24B;
  private static final int REGISTER_CONTROLS = 0x24F;

  public GravisUltraSound(Machine machine, boolean failOnUnhandledPort) {
    super(machine, failOnUnhandledPort);
  }

  @Override
  public void initPortHandlers(IOPortDispatcher ioPortDispatcher) {
    ioPortDispatcher.addIOPortHandler(MIX_CONTROL_REGISTER, this);
    ioPortDispatcher.addIOPortHandler(READ_DATA_OR_TRIGGER_STATUS, this);
    // Not sure what those are but some programs search the card in those ports as well
    ioPortDispatcher.addIOPortHandler(0x243, this);
    ioPortDispatcher.addIOPortHandler(0x280, this);
    ioPortDispatcher.addIOPortHandler(0x281, this);
    ioPortDispatcher.addIOPortHandler(0x283, this);
    ioPortDispatcher.addIOPortHandler(0x2C0, this);
    ioPortDispatcher.addIOPortHandler(0x2C1, this);
    ioPortDispatcher.addIOPortHandler(0x2C3, this);

    ioPortDispatcher.addIOPortHandler(IRQ_STATUS_REGISTER, this);
    ioPortDispatcher.addIOPortHandler(TIMER_CONTROL_REGISTER, this);
    ioPortDispatcher.addIOPortHandler(IRQ_CONTROL_REGISTER, this);
    ioPortDispatcher.addIOPortHandler(REGISTER_CONTROLS, this);
  }
}
