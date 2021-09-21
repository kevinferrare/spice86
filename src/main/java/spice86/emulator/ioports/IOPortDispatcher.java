package spice86.emulator.ioports;

import java.util.HashMap;
import java.util.Map;

import spice86.emulator.errors.InvalidOperationException;
import spice86.emulator.machine.Machine;

/**
 * Handles calling the correct dispatcher depending on port number for I/O reads and writes.
 */
public class IOPortDispatcher {
  private Machine machine;
  private Map<Integer, IOPortHandler> ioPortHandlers = new HashMap<>();

  public IOPortDispatcher(Machine machine) {
    this.machine = machine;
  }

  public void addIOPortHandler(int port, IOPortHandler ioPortHandler) {
    ioPortHandlers.put(port, ioPortHandler);
  }

  public int inb(int port) throws InvalidOperationException {
    if (ioPortHandlers.containsKey(port)) {
      return ioPortHandlers.get(port).inb(port);
    }
    throw new UnhandledIOPortException(machine, port);
  }

  public int inw(int port) throws InvalidOperationException {
    if (ioPortHandlers.containsKey(port)) {
      return ioPortHandlers.get(port).inw(port);
    }
    throw new UnhandledIOPortException(machine, port);
  }

  public void outb(int port, int value) throws InvalidOperationException {
    if (ioPortHandlers.containsKey(port)) {
      ioPortHandlers.get(port).outb(port, value);
    } else {
      throw new UnhandledIOPortException(machine, port);
    }
  }

  public void outw(int port, int value) throws InvalidOperationException {
    if (ioPortHandlers.containsKey(port)) {
      ioPortHandlers.get(port).outw(port, value);
    } else {
      throw new UnhandledIOPortException(machine, port);
    }
  }
}
