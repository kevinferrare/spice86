package spice86.emulator.reverseengineer;

import spice86.emulator.cpu.State;
import spice86.emulator.machine.Machine;

/**
 * memory based data structure where the base address is the DS segment
 */
public class MemoryBasedDataStructureWithDsBaseAddress extends MemoryBasedDataStructureWithBaseAddressProvider {
  private State state;

  public MemoryBasedDataStructureWithDsBaseAddress(Machine machine) {
    super(machine.getMemory());
    this.state = machine.getCpu().getState();
  }

  @Override
  public int getBaseAddress() {
    return state.getDS() * 0x10;
  }

}
