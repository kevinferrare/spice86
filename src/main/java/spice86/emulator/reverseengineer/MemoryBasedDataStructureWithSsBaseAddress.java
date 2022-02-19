package spice86.emulator.reverseengineer;

import spice86.emulator.cpu.SegmentRegisters;
import spice86.emulator.machine.Machine;

/**
 * memory based data structure where the base address is the SS segment
 */
public class MemoryBasedDataStructureWithSsBaseAddress extends MemoryBasedDataStructureWithSegmentRegisterBaseAddress {
  public MemoryBasedDataStructureWithSsBaseAddress(Machine machine) {
    super(machine, SegmentRegisters.SS_INDEX);
  }
}
