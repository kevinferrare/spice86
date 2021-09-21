package spice86.emulator.devices.sound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.errors.InvalidOperationException;
import spice86.emulator.ioports.DummyIOPortHandler;
import spice86.emulator.ioports.IOPortDispatcher;
import spice86.utils.ConvertUtils;

/**
 * PC speaker implementation. Does not produce any sound, just handles the bare minimum to make programs run.
 */
public class PcSpeaker extends DummyIOPortHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(PcSpeaker.class);
  private static final int PC_SPEAKER_PORT_NUMBER = 0x61;

  private int value;

  @Override
  public void initPortHandlers(IOPortDispatcher ioPortDispatcher) {
    ioPortDispatcher.addIOPortHandler(PC_SPEAKER_PORT_NUMBER, this);
  }

  @Override
  public int inb(int port) throws InvalidOperationException {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("PC Speaker get value {}", ConvertUtils.toHex8(this.value));
    }
    return this.value;
  }

  @Override
  public void outb(int port, int value) throws InvalidOperationException {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("PC Speaker set value {}", ConvertUtils.toHex8(value));
    }
    this.value = value;
  }
}
