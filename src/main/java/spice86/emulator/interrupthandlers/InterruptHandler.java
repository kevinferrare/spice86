package spice86.emulator.interrupthandlers;

import spice86.emulator.callback.Callback;
import spice86.emulator.callback.IndexBasedDispatcher;
import spice86.emulator.cpu.Cpu;
import spice86.emulator.cpu.Flags;
import spice86.emulator.cpu.State;
import spice86.emulator.errors.UnhandledOperationException;
import spice86.emulator.machine.Machine;
import spice86.emulator.memory.Memory;
import spice86.utils.CheckedRunnable;

/**
 * Base class for all interrupt handlers rewritten in java.<br/>
 * Provides an access to the machine as well.
 */
public abstract class InterruptHandler
    extends IndexBasedDispatcher<CheckedRunnable<UnhandledOperationException>>
    implements Callback {
  // Protected visibility because they are used by almost all implementations
  protected Machine machine;
  protected Memory memory;
  protected Cpu cpu;
  protected State state;

  protected InterruptHandler(Machine machine) {
    this.machine = machine;
    this.memory = machine.getMemory();
    this.cpu = machine.getCpu();
    this.state = cpu.getState();
  }

  @Override
  protected UnhandledOperationException generateUnhandledOperationException(int index) {
    return new UnhandledInterruptException(machine, getIndex(), index);
  }

  /**
   * Sets the carry flag both in the state and in the interrupt stack.<br/>
   * This is a technique used by some dos interrupt handlers to transmit some flags even after iret.<br/>
   * Not blindly copying all flags because the interrupt flag value needs to be kept.
   * 
   * @param value
   * @param setOnStack
   *          whether to modify the flag on the call stack as well. Needed for when called via int
   */
  protected void setCarryFlag(boolean value, boolean setOnStack) {
    state.setCarryFlag(value);
    if (setOnStack) {
      cpu.setFlagOnInterruptStack(Flags.CARRY, value);
    }
  }

  protected void setZeroFlag(boolean value, boolean setOnStack) {
    state.setZeroFlag(value);
    if (setOnStack) {
      cpu.setFlagOnInterruptStack(Flags.ZERO, value);
    }
  }
}
