package spice86.emulator.ioports;

import spice86.emulator.errors.InvalidOperationException;

/**
 * An IO port handler that does nothing
 */
public abstract class DummyIOPortHandler implements IOPortHandler {

  @Override
  public int inb(int port) throws InvalidOperationException {
    return 0;
  }

  @Override
  public int inw(int port) throws InvalidOperationException {
    return 0;
  }

  @Override
  public void outb(int port, int value) throws InvalidOperationException {
    // Do nothing
  }

  @Override
  public void outw(int port, int value) throws InvalidOperationException {
    // Do nothing
  }

}
