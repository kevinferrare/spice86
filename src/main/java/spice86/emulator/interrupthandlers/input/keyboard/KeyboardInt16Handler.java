package spice86.emulator.interrupthandlers.input.keyboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.errors.UnhandledOperationException;
import spice86.emulator.interrupthandlers.InterruptHandler;
import spice86.emulator.machine.Machine;

/**
 * Interface between the keyboard and the emulator.<br/>
 * Re-implements int16.<br/>
 * Triggers int9 to the CPU.<br/>
 * https://stanislavs.org/helppc/int_16.html
 */
public class KeyboardInt16Handler extends InterruptHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(KeyboardInt16Handler.class);

  private BiosKeyboardBuffer biosKeyboardBuffer;

  public KeyboardInt16Handler(Machine machine, BiosKeyboardBuffer biosKeyboardBuffer) {
    super(machine);
    this.biosKeyboardBuffer = biosKeyboardBuffer;
    super.dispatchTable.put(0x00, this::getKeystroke);
    super.dispatchTable.put(0x01, () -> getKeystrokeStatus(true));
  }

  public Integer getNextKeyCode() {
    return biosKeyboardBuffer.getKeyCode();
  }

  public void getKeystroke() {
    LOGGER.info("READ KEY STROKE");
    Integer keyCode = getNextKeyCode();
    if (keyCode == null) {
      keyCode = 0;
    }
    // AH = keyboard scan code
    // AL = ASCII character or zero if special function key
    state.setAX(keyCode);
  }

  public void getKeystrokeStatus(boolean calledFromVm) {
    LOGGER.info("KEY STROKE STATUS");
    // ZF = 0 if a key pressed (even Ctrl-Break)
    // AX = 0 if no scan code is available
    // AH = scan code
    // AL = ASCII character or zero if special function key
    if (biosKeyboardBuffer.empty()) {
      setZeroFlag(true, calledFromVm);
      state.setAX(0);
    } else {
      Integer keyCode = biosKeyboardBuffer.getKeyCode();
      setZeroFlag(false, calledFromVm);
      state.setAX(keyCode);
    }
  }

  @Override
  public void run() throws UnhandledOperationException {
    int operation = state.getAH();
    this.run(operation);
  }

  @Override
  public int getIndex() {
    return 0x16;
  }
}
