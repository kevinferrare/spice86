package spice86.emulator.ioports;

import java.util.HashMap;
import java.util.Map;

import spice86.emulator.errors.InvalidOperationException;
import spice86.emulator.machine.Machine;

/**
 * Handles calling the correct dispatcher depending on port number for I/O reads and writes.
 */
public class IOPortDispatcher extends DefaultIOPortHandler {
  private Map<Integer, IOPortHandler> ioPortHandlers = new HashMap<>();

  public IOPortDispatcher(Machine machine, boolean failOnUnhandledPort) {
    super(machine, failOnUnhandledPort);
    this.failOnUnhandledPort = failOnUnhandledPort;
  }

  public void addIOPortHandler(int port, IOPortHandler ioPortHandler) {
    ioPortHandlers.put(port, ioPortHandler);
  }

  public int inb(int port) throws InvalidOperationException {
    if (ioPortHandlers.containsKey(port)) {
      return ioPortHandlers.get(port).inb(port);
    }
    return super.inb(port);
  }

  public int inw(int port) throws InvalidOperationException {
    if (ioPortHandlers.containsKey(port)) {
      return ioPortHandlers.get(port).inw(port);
    }
    return super.inw(port);
  }

  public void outb(int port, int value) throws InvalidOperationException {
    if (ioPortHandlers.containsKey(port)) {
      ioPortHandlers.get(port).outb(port, value);
    } else {
      super.outb(port, value);
    }
  }

  public void outw(int port, int value) throws InvalidOperationException {
    if (ioPortHandlers.containsKey(port)) {
      ioPortHandlers.get(port).outw(port, value);
    } else {
      super.outw(port, value);
    }
  }

  @Override
  public void initPortHandlers(IOPortDispatcher ioPortDispatcher) {
    // Nothing to implement, just for API compatibility.
  }
}
