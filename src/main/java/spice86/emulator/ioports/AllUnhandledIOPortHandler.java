package spice86.emulator.ioports;

import spice86.emulator.cpu.Cpu;
import spice86.emulator.errors.InvalidOperationException;
import spice86.emulator.machine.Machine;
import spice86.emulator.memory.Memory;

/**
 * Base class for IO port handlers, by default all operations throw an unhandled operation exception.
 */
public abstract class AllUnhandledIOPortHandler implements IOPortHandler {
  protected Machine machine;
  protected Memory memory;
  protected Cpu cpu;

  protected AllUnhandledIOPortHandler(Machine machine) {
    this.machine = machine;
    this.memory = machine.getMemory();
    this.cpu = machine.getCpu();
  }

  @Override
  public int inb(int port) throws InvalidOperationException {
    throw new UnhandledIOPortException(machine, port);
  }

  @Override
  public int inw(int port) throws InvalidOperationException {
    throw new UnhandledIOPortException(machine, port);
  }

  @Override
  public void outb(int port, int value) throws InvalidOperationException {
    throw new UnhandledIOPortException(machine, port);
  }

  @Override
  public void outw(int port, int value) throws InvalidOperationException {
    throw new UnhandledIOPortException(machine, port);
  }
}
