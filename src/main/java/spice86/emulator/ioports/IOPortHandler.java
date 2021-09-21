package spice86.emulator.ioports;

import spice86.emulator.errors.InvalidOperationException;

/**
 * Interface classes handling port data through IOPortDispatcher have to follow.
 */
public interface IOPortHandler {
  public void initPortHandlers(IOPortDispatcher ioPortDispatcher);

  public int inb(int port) throws InvalidOperationException;

  public int inw(int port) throws InvalidOperationException;

  public void outb(int port, int value) throws InvalidOperationException;

  public void outw(int port, int value) throws InvalidOperationException;
}
