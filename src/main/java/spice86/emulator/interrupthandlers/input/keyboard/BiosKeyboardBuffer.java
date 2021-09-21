package spice86.emulator.interrupthandlers.input.keyboard;

import spice86.emulator.memory.Memory;
import spice86.emulator.reverseengineer.MemoryBasedDataStructureWithBaseAddress;

/**
 * Bios keyboard buffer implementation.<br/>
 * Some documentation: https://jeffpar.github.io/kbarchive/kb/060/Q60140/
 */
public class BiosKeyboardBuffer extends MemoryBasedDataStructureWithBaseAddress {
  private static final int START = 0x480;
  private static final int END = 0x482;
  private static final int HEAD = 0x41A;
  private static final int TAIL = 0x41C;
  private static final int INITIAL_START_ADDRESS = 0x41E;
  private static final int INITIAL_LENGTH = 0x20;

  public BiosKeyboardBuffer(Memory memory) {
    super(memory, 0);
  }

  public void init() {
    this.setStartAddress(INITIAL_START_ADDRESS);
    this.setEndAddress(INITIAL_START_ADDRESS + INITIAL_LENGTH);
    this.setHeadAddress(INITIAL_START_ADDRESS);
    this.setTailAddress(INITIAL_START_ADDRESS);
  }

  public int getStartAddress() {
    return this.getUint16(START);
  }

  public void setStartAddress(int value) {
    this.setUint16(START, value);
  }

  public int getEndAddress() {
    return this.getUint16(END);
  }

  public void setEndAddress(int value) {
    this.setUint16(END, value);
  }

  public int getHeadAddress() {
    return this.getUint16(HEAD);
  }

  public void setHeadAddress(int value) {
    this.setUint16(HEAD, value);
  }

  public int getTailAddress() {
    return this.getUint16(TAIL);
  }

  public void setTailAddress(int value) {
    this.setUint16(TAIL, value);
  }

  public boolean addKeyCode(int code) {
    int tail = getTailAddress();
    int newTail = advancePointer(tail);
    if (newTail == getHeadAddress()) {
      // buffer full
      return false;
    }
    this.setUint16(tail, code);
    this.setTailAddress(newTail);
    return true;
  }

  public Integer getKeyCode() {
    int head = getHeadAddress();
    if (empty()) {
      return null;
    }
    int newHead = advancePointer(getHeadAddress());
    this.setHeadAddress(newHead);
    return this.getUint16(head);
  }

  public boolean empty() {
    int head = getHeadAddress();
    int tail = getTailAddress();
    return head == tail;
  }

  private int advancePointer(int value) {
    int res = value + 2;
    if (res >= getEndAddress()) {
      return getStartAddress();
    }
    return res;
  }
}
