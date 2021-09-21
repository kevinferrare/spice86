package spice86.emulator.devices.input.joystick;

import spice86.emulator.ioports.DummyIOPortHandler;
import spice86.emulator.ioports.IOPortDispatcher;

/**
 * Joystick implementation. Emulates an unplugged joystick for now.
 */
public class Joystick extends DummyIOPortHandler {
  private static final int JOYSTIC_POSITON_AND_STATUS = 0x201;

  @Override
  public void initPortHandlers(IOPortDispatcher ioPortDispatcher) {
    ioPortDispatcher.addIOPortHandler(JOYSTIC_POSITON_AND_STATUS, this);
  }
}
