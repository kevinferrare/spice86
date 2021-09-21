package spice86.emulator.devices.sound;

import spice86.emulator.ioports.DummyIOPortHandler;
import spice86.emulator.ioports.IOPortDispatcher;

/**
 * Midi implementation. Emulates an absent card :)
 */
public class Midi extends DummyIOPortHandler {
  private static final int MIDI_INTERFACE_1 = 0x330;
  private static final int MIDI_INTERFACE_2 = 0x331;

  @Override
  public void initPortHandlers(IOPortDispatcher ioPortDispatcher) {
    ioPortDispatcher.addIOPortHandler(MIDI_INTERFACE_1, this);
    ioPortDispatcher.addIOPortHandler(MIDI_INTERFACE_2, this);
  }
}
