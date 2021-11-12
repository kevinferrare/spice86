package spice86.emulator.reverseengineer;

import spice86.emulator.cpu.SegmentRegisters;
import spice86.emulator.machine.Machine;

/**
 * memory based data structure where the base address is the CS segment
 */
public class MemoryBasedDataStructureWithCsBaseAddress extends MemoryBasedDataStructureWithSegmentRegisterBaseAddress {
  public MemoryBasedDataStructureWithCsBaseAddress(Machine machine) {
    super(machine, SegmentRegisters.CS_INDEX);
  }
}
