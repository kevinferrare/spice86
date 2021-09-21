package spice86.emulator.devices.input.keyboard;

import static spice86.utils.ConvertUtils.uint8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.input.KeyCode;
import spice86.emulator.errors.InvalidOperationException;
import spice86.emulator.ioports.AllUnhandledIOPortHandler;
import spice86.emulator.ioports.IOPortDispatcher;
import spice86.emulator.machine.Machine;
import spice86.ui.Gui;

/**
 * Basic implementation of a keyboard
 */
public class Keyboard extends AllUnhandledIOPortHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(Keyboard.class);

  private static final int KEYBOARD_IO_PORT = 0x60;

  private KeyScancodeConverter keyScancodeConverter = new KeyScancodeConverter();
  private Gui gui;

  public Keyboard(Machine machine, Gui gui) {
    super(machine);
    this.gui = gui;
    if (gui != null) {
      gui.setOnKeyPressedEvent(this::onKeyEvent);
      gui.setOnKeyReleasedEvent(this::onKeyEvent);
    }
  }

  public void onKeyEvent() {
    cpu.externalInterrupt(9);
  }

  public Integer getScancode() {
    KeyCode keyCode = gui.getLastKeyCode();
    Integer scancode;
    if (gui.isKeyPressed(keyCode)) {
      scancode = keyScancodeConverter.getKeyPressedScancode(keyCode);
      LOGGER.info("Getting scancode. Key pressed {} scancode {}", keyCode, scancode);
    } else {
      scancode = keyScancodeConverter.getKeyReleasedScancode(keyCode);
      LOGGER.info("Getting scancode. Key released {} scancode {}", keyCode, scancode);
    }
    if (scancode == null) {
      return null;
    }
    return uint8(scancode);
  }

  @Override
  public void initPortHandlers(IOPortDispatcher ioPortDispatcher) {
    ioPortDispatcher.addIOPortHandler(KEYBOARD_IO_PORT, this);
  }

  @Override
  public int inb(int port) throws InvalidOperationException {
    Integer scancode = getScancode();
    if (scancode == null) {
      return 0;
    }
    return scancode;
  }
}
