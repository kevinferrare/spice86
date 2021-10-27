package spice86.emulator.devices.sound;

import spice86.emulator.errors.InvalidOperationException;
import spice86.emulator.ioports.DefaultIOPortHandler;
import spice86.emulator.ioports.IOPortDispatcher;
import spice86.emulator.machine.Machine;

/**
 * MPU401 (Midi) implementation. Emulates an absent card :)
 */
public class Midi extends DefaultIOPortHandler {
  private static final int DATA = 0x330;
  private static final int COMMAND = 0x331;

  public Midi(Machine machine, boolean failOnUnhandledPort) {
    super(machine, failOnUnhandledPort);
  }

  @Override
  public void initPortHandlers(IOPortDispatcher ioPortDispatcher) {
    ioPortDispatcher.addIOPortHandler(DATA, this);
    ioPortDispatcher.addIOPortHandler(COMMAND, this);
  }

  @Override
  public int inb(int port) throws InvalidOperationException {
    if (port == DATA) {
      return readData();
    } else {
      return readStatus();
    }
  }

  @Override
  public void outb(int port, int value) throws InvalidOperationException {
    if (port == DATA) {
      writeData(value);
    } else {
      writeCommand(value);
    }
  }

  public void writeData(int value) {
    // Not implemented yet
  }

  public void writeCommand(int value) {
    // Not implemented yet
  }

  public int readData() {
    return 0;
  }

  public int readStatus() {
    return 0;
  }
}
