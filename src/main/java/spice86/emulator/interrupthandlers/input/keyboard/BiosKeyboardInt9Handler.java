package spice86.emulator.interrupthandlers.input.keyboard;

import spice86.emulator.devices.input.keyboard.KeyScancodeConverter;
import spice86.emulator.devices.input.keyboard.Keyboard;
import spice86.emulator.errors.UnhandledOperationException;
import spice86.emulator.interrupthandlers.InterruptHandler;
import spice86.emulator.machine.Machine;

/**
 * Crude implementation of int9
 */
public class BiosKeyboardInt9Handler extends InterruptHandler {
  private KeyScancodeConverter keyScancodeConverter = new KeyScancodeConverter();
  private BiosKeyboardBuffer biosKeyboardBuffer;
  private Keyboard keyboard;

  public BiosKeyboardInt9Handler(Machine machine) {
    super(machine);
    this.keyboard = machine.getKeyboard();
    this.biosKeyboardBuffer = new BiosKeyboardBuffer(machine.getMemory());
    biosKeyboardBuffer.init();
  }

  public BiosKeyboardBuffer getBiosKeyboardBuffer() {
    return biosKeyboardBuffer;
  }

  @Override
  public void run() throws UnhandledOperationException {
    Integer scancode = keyboard.getScancode();
    if (scancode == null) {
      return;
    }
    Integer ascii = keyScancodeConverter.getAsciiCode(scancode);
    if (ascii == null) {
      ascii = 0;
    }
    biosKeyboardBuffer.addKeyCode((scancode << 8) | ascii);
  }

  @Override
  public int getIndex() {
    return 0x9;
  }
}
