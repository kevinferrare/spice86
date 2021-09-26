package spice86.emulator.devices.input.joystick;

import spice86.emulator.ioports.DefaultIOPortHandler;
import spice86.emulator.ioports.IOPortDispatcher;
import spice86.emulator.machine.Machine;

/**
 * Joystick implementation. Emulates an unplugged joystick for now.
 */
public class Joystick extends DefaultIOPortHandler {
  private static final int JOYSTIC_POSITON_AND_STATUS = 0x201;

  public Joystick(Machine machine, boolean failOnUnhandledPort) {
    super(machine, failOnUnhandledPort);
  }

  @Override
  public void initPortHandlers(IOPortDispatcher ioPortDispatcher) {
    ioPortDispatcher.addIOPortHandler(JOYSTIC_POSITON_AND_STATUS, this);
  }
}
