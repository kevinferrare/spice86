package spice86.emulator.cpu;

import static spice86.utils.ConvertUtils.uint16;

import spice86.emulator.memory.Memory;

/**
 * Manipulation of the standard stack accessible via SS:SP.
 */
public class Stack {
  private Memory memory;
  private State state;

  public Stack(Memory memory, State state) {
    this.memory = memory;
    this.state = state;
  }

  public void push(int value) {
    int sp = uint16(state.getSP() - 2);
    state.setSP(sp);
    memory.setUint16(state.getStackPhysicalAddress(), value);
  }

  public int pop() {
    int res = memory.getUint16(state.getStackPhysicalAddress());
    state.setSP(state.getSP() + 2);
    return res;
  }

  public int peek(int index) {
    return memory.getUint16(state.getStackPhysicalAddress() + index);
  }

  public void poke(int index, int value) {
    memory.setUint16(state.getStackPhysicalAddress() + index, value);
  }

}
