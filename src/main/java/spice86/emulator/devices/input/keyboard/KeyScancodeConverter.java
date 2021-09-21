package spice86.emulator.devices.input.keyboard;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.input.KeyCode;

/**
 * Maps java KeyCode to Scancodes expected by int16.<br/>
 * No handling of shift / alt yet.<br/>
 * http://www.ctyme.com/intr/rb-0045.htm#Table6<br/>
 */
public class KeyScancodeConverter {

  private static final Map<KeyCode, Integer> KEY_PRESSED_SCANCODE = new EnumMap<>(KeyCode.class);
  private static final Map<Integer, Integer> SCANCODE_TO_ASCII = new HashMap<>();
  static {
    // Some keys are not supported by javafx so not putting them.
    KEY_PRESSED_SCANCODE.put(KeyCode.CONTROL, 0x1D);
    KEY_PRESSED_SCANCODE.put(KeyCode.SHIFT, 0x2A);
    KEY_PRESSED_SCANCODE.put(KeyCode.F1, 0x3B);
    KEY_PRESSED_SCANCODE.put(KeyCode.F2, 0x3C);
    KEY_PRESSED_SCANCODE.put(KeyCode.F3, 0x3D);
    KEY_PRESSED_SCANCODE.put(KeyCode.F4, 0x3E);
    KEY_PRESSED_SCANCODE.put(KeyCode.F5, 0x3F);
    KEY_PRESSED_SCANCODE.put(KeyCode.F6, 0x40);
    KEY_PRESSED_SCANCODE.put(KeyCode.F7, 0x41);
    KEY_PRESSED_SCANCODE.put(KeyCode.F8, 0x42);
    KEY_PRESSED_SCANCODE.put(KeyCode.F9, 0x43);
    KEY_PRESSED_SCANCODE.put(KeyCode.F10, 0x44);
    KEY_PRESSED_SCANCODE.put(KeyCode.F11, 0x57);
    KEY_PRESSED_SCANCODE.put(KeyCode.F12, 0x58);
    KEY_PRESSED_SCANCODE.put(KeyCode.NUM_LOCK, 0x45);
    KEY_PRESSED_SCANCODE.put(KeyCode.ALT, 0x38);
    KEY_PRESSED_SCANCODE.put(KeyCode.A, 0x1E);
    KEY_PRESSED_SCANCODE.put(KeyCode.B, 0x30);
    KEY_PRESSED_SCANCODE.put(KeyCode.C, 0x2E);
    KEY_PRESSED_SCANCODE.put(KeyCode.D, 0x20);
    KEY_PRESSED_SCANCODE.put(KeyCode.E, 0x12);
    KEY_PRESSED_SCANCODE.put(KeyCode.F, 0x21);
    KEY_PRESSED_SCANCODE.put(KeyCode.G, 0x22);
    KEY_PRESSED_SCANCODE.put(KeyCode.H, 0x23);
    KEY_PRESSED_SCANCODE.put(KeyCode.I, 0x17);
    KEY_PRESSED_SCANCODE.put(KeyCode.J, 0x24);
    KEY_PRESSED_SCANCODE.put(KeyCode.K, 0x25);
    KEY_PRESSED_SCANCODE.put(KeyCode.L, 0x26);
    KEY_PRESSED_SCANCODE.put(KeyCode.M, 0x32);
    KEY_PRESSED_SCANCODE.put(KeyCode.N, 0x31);
    KEY_PRESSED_SCANCODE.put(KeyCode.O, 0x18);
    KEY_PRESSED_SCANCODE.put(KeyCode.P, 0x19);
    KEY_PRESSED_SCANCODE.put(KeyCode.Q, 0x10);
    KEY_PRESSED_SCANCODE.put(KeyCode.R, 0x13);
    KEY_PRESSED_SCANCODE.put(KeyCode.S, 0x1F);
    KEY_PRESSED_SCANCODE.put(KeyCode.T, 0x14);
    KEY_PRESSED_SCANCODE.put(KeyCode.U, 0x16);
    KEY_PRESSED_SCANCODE.put(KeyCode.V, 0x2F);
    KEY_PRESSED_SCANCODE.put(KeyCode.W, 0x11);
    KEY_PRESSED_SCANCODE.put(KeyCode.X, 0x2D);
    KEY_PRESSED_SCANCODE.put(KeyCode.Y, 0x15);
    KEY_PRESSED_SCANCODE.put(KeyCode.Z, 0x2C);
    KEY_PRESSED_SCANCODE.put(KeyCode.DIGIT0, 0xB);
    KEY_PRESSED_SCANCODE.put(KeyCode.DIGIT1, 0x2);
    KEY_PRESSED_SCANCODE.put(KeyCode.DIGIT2, 0x3);
    KEY_PRESSED_SCANCODE.put(KeyCode.DIGIT3, 0x4);
    KEY_PRESSED_SCANCODE.put(KeyCode.DIGIT4, 0x5);
    KEY_PRESSED_SCANCODE.put(KeyCode.DIGIT5, 0x6);
    KEY_PRESSED_SCANCODE.put(KeyCode.DIGIT6, 0x7);
    KEY_PRESSED_SCANCODE.put(KeyCode.DIGIT7, 0x8);
    KEY_PRESSED_SCANCODE.put(KeyCode.DIGIT8, 0x9);
    KEY_PRESSED_SCANCODE.put(KeyCode.DIGIT9, 0xA);
    KEY_PRESSED_SCANCODE.put(KeyCode.ESCAPE, 0x1);
    KEY_PRESSED_SCANCODE.put(KeyCode.SPACE, 0x39);
    KEY_PRESSED_SCANCODE.put(KeyCode.QUOTE, 0x28);
    KEY_PRESSED_SCANCODE.put(KeyCode.COMMA, 0x33);
    KEY_PRESSED_SCANCODE.put(KeyCode.PERIOD, 0x34);
    KEY_PRESSED_SCANCODE.put(KeyCode.SLASH, 0x35);
    KEY_PRESSED_SCANCODE.put(KeyCode.SEMICOLON, 0x27);
    KEY_PRESSED_SCANCODE.put(KeyCode.EQUALS, 0xD);
    KEY_PRESSED_SCANCODE.put(KeyCode.OPEN_BRACKET, 0x1A);
    KEY_PRESSED_SCANCODE.put(KeyCode.BACK_SLASH, 0x2B);
    KEY_PRESSED_SCANCODE.put(KeyCode.CLOSE_BRACKET, 0x1B);
    KEY_PRESSED_SCANCODE.put(KeyCode.MINUS, 0xC);
    KEY_PRESSED_SCANCODE.put(KeyCode.BACK_QUOTE, 0x29);
    KEY_PRESSED_SCANCODE.put(KeyCode.BACK_SPACE, 0xE);
    KEY_PRESSED_SCANCODE.put(KeyCode.ENTER, 0x1C);
    KEY_PRESSED_SCANCODE.put(KeyCode.TAB, 0xF);
    KEY_PRESSED_SCANCODE.put(KeyCode.ADD, 0x4E);
    KEY_PRESSED_SCANCODE.put(KeyCode.SUBTRACT, 0x4A);
    KEY_PRESSED_SCANCODE.put(KeyCode.END, 0x4F);
    KEY_PRESSED_SCANCODE.put(KeyCode.DOWN, 0x50);
    KEY_PRESSED_SCANCODE.put(KeyCode.PAGE_DOWN, 0x51);
    KEY_PRESSED_SCANCODE.put(KeyCode.LEFT, 0x4B);
    KEY_PRESSED_SCANCODE.put(KeyCode.RIGHT, 0x4D);
    KEY_PRESSED_SCANCODE.put(KeyCode.HOME, 0x47);
    KEY_PRESSED_SCANCODE.put(KeyCode.UP, 0x48);
    KEY_PRESSED_SCANCODE.put(KeyCode.PAGE_UP, 0x49);
    KEY_PRESSED_SCANCODE.put(KeyCode.INSERT, 0x52);
    KEY_PRESSED_SCANCODE.put(KeyCode.DELETE, 0x53);
    KEY_PRESSED_SCANCODE.put(KeyCode.DIGIT5, 0x4C);
    KEY_PRESSED_SCANCODE.put(KeyCode.MULTIPLY, 0x37);

    SCANCODE_TO_ASCII.put(0x01, 0x1B);
    SCANCODE_TO_ASCII.put(0x02, 0x31);
    SCANCODE_TO_ASCII.put(0x03, 0x32);
    SCANCODE_TO_ASCII.put(0x04, 0x33);
    SCANCODE_TO_ASCII.put(0x05, 0x34);
    SCANCODE_TO_ASCII.put(0x06, 0x35);
    SCANCODE_TO_ASCII.put(0x07, 0x36);
    SCANCODE_TO_ASCII.put(0x08, 0x37);
    SCANCODE_TO_ASCII.put(0x09, 0x38);
    SCANCODE_TO_ASCII.put(0x0A, 0x39);
    SCANCODE_TO_ASCII.put(0x0B, 0x30);
    SCANCODE_TO_ASCII.put(0x0C, 0x2D);
    SCANCODE_TO_ASCII.put(0x0D, 0x3D);
    SCANCODE_TO_ASCII.put(0x0E, 0x08);
    SCANCODE_TO_ASCII.put(0x0F, 0x09);
    SCANCODE_TO_ASCII.put(0x10, 0x71);
    SCANCODE_TO_ASCII.put(0x11, 0x77);
    SCANCODE_TO_ASCII.put(0x12, 0x65);
    SCANCODE_TO_ASCII.put(0x13, 0x72);
    SCANCODE_TO_ASCII.put(0x14, 0x74);
    SCANCODE_TO_ASCII.put(0x15, 0x79);
    SCANCODE_TO_ASCII.put(0x16, 0x75);
    SCANCODE_TO_ASCII.put(0x17, 0x69);
    SCANCODE_TO_ASCII.put(0x18, 0x6F);
    SCANCODE_TO_ASCII.put(0x19, 0x70);
    SCANCODE_TO_ASCII.put(0x1A, 0x5B);
    SCANCODE_TO_ASCII.put(0x1B, 0x5D);
    SCANCODE_TO_ASCII.put(0x1C, 0x0D);
    SCANCODE_TO_ASCII.put(0x1E, 0x61);
    SCANCODE_TO_ASCII.put(0x1F, 0x73);
    SCANCODE_TO_ASCII.put(0x20, 0x64);
    SCANCODE_TO_ASCII.put(0x21, 0x66);
    SCANCODE_TO_ASCII.put(0x22, 0x67);
    SCANCODE_TO_ASCII.put(0x23, 0x68);
    SCANCODE_TO_ASCII.put(0x24, 0x6A);
    SCANCODE_TO_ASCII.put(0x25, 0x6B);
    SCANCODE_TO_ASCII.put(0x26, 0x6C);
    SCANCODE_TO_ASCII.put(0x27, 0x3B);
    SCANCODE_TO_ASCII.put(0x28, 0x27);
    SCANCODE_TO_ASCII.put(0x29, 0x60);
    SCANCODE_TO_ASCII.put(0x2B, 0x5C);
    SCANCODE_TO_ASCII.put(0x2C, 0x7A);
    SCANCODE_TO_ASCII.put(0x2D, 0x78);
    SCANCODE_TO_ASCII.put(0x2E, 0x63);
    SCANCODE_TO_ASCII.put(0x2F, 0x76);
    SCANCODE_TO_ASCII.put(0x30, 0x62);
    SCANCODE_TO_ASCII.put(0x31, 0x6E);
    SCANCODE_TO_ASCII.put(0x32, 0x6D);
    SCANCODE_TO_ASCII.put(0x33, 0x2C);
    SCANCODE_TO_ASCII.put(0x34, 0x2E);
    SCANCODE_TO_ASCII.put(0x35, 0x2F);
    SCANCODE_TO_ASCII.put(0x37, 0x2A);
    SCANCODE_TO_ASCII.put(0x39, 0x20);
    SCANCODE_TO_ASCII.put(0x4A, 0x2D);
    SCANCODE_TO_ASCII.put(0x4C, 0x35);
    SCANCODE_TO_ASCII.put(0x4E, 0x2B);
  }

  public Integer getKeyPressedScancode(KeyCode keyCode) {
    return KEY_PRESSED_SCANCODE.get(keyCode);
  }

  public Integer getKeyReleasedScancode(KeyCode keyCode) {
    Integer pressed = getKeyPressedScancode(keyCode);
    if (pressed != null) {
      return pressed + 0x80;
    }
    return null;
  }

  public Integer getAsciiCode(int scancode) {
    int keypressedScancode = scancode;
    if (keypressedScancode > 0x7F) {
      keypressedScancode -= 0x80;
    }
    return SCANCODE_TO_ASCII.get(keypressedScancode);
  }
}
