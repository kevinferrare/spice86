package spice86.emulator.devices.sound;

import spice86.emulator.ioports.DefaultIOPortHandler;
import spice86.emulator.ioports.IOPortDispatcher;
import spice86.emulator.machine.Machine;

/**
 * Midi implementation. Emulates an absent card :)
 */
public class Midi extends DefaultIOPortHandler {
  private static final int MIDI_INTERFACE_1 = 0x330;
  private static final int MIDI_INTERFACE_2 = 0x331;

  public Midi(Machine machine, boolean failOnUnhandledPort) {
    super(machine, failOnUnhandledPort);
  }

  @Override
  public void initPortHandlers(IOPortDispatcher ioPortDispatcher) {
    ioPortDispatcher.addIOPortHandler(MIDI_INTERFACE_1, this);
    ioPortDispatcher.addIOPortHandler(MIDI_INTERFACE_2, this);
  }
}
