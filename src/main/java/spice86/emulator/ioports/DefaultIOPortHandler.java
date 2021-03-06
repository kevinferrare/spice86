package spice86.emulator.ioports;

import spice86.emulator.cpu.Cpu;
import spice86.emulator.errors.InvalidOperationException;
import spice86.emulator.machine.Machine;
import spice86.emulator.memory.Memory;

/**
 * Base class for IO port handlers.<br/>
 * All operations throw an unhandled operation exception if failOnUnhandledPort is true, or do nothing if false.<br/>
 * If failOnUnhandledPort, inb and inw will return 0.
 */
public abstract class DefaultIOPortHandler implements IOPortHandler {
  protected Machine machine;
  protected Memory memory;
  protected Cpu cpu;
  protected boolean failOnUnhandledPort;

  protected DefaultIOPortHandler(Machine machine, boolean failOnUnhandledPort) {
    this.machine = machine;
    this.memory = machine.getMemory();
    this.cpu = machine.getCpu();
    this.failOnUnhandledPort = failOnUnhandledPort;
  }

  @Override
  public int inb(int port) throws InvalidOperationException {
    return onUnandledIn(port);
  }

  @Override
  public int inw(int port) throws InvalidOperationException {
    return onUnandledIn(port);
  }

  @Override
  public void outb(int port, int value) throws InvalidOperationException {
    onUnhandledPort(port);
  }

  @Override
  public void outw(int port, int value) throws InvalidOperationException {
    onUnhandledPort(port);
  }

  protected int onUnandledIn(int port) throws UnhandledIOPortException {
    onUnhandledPort(port);
    return 0;
  }

  protected void onUnhandledPort(int port) throws UnhandledIOPortException {
    if (failOnUnhandledPort) {
      throw new UnhandledIOPortException(machine, port);
    }
  }
}
