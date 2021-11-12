package spice86.emulator.reverseengineer;

import spice86.emulator.cpu.SegmentRegisters;
import spice86.emulator.machine.Machine;

/**
 * memory based data structure where the base address is the DS segment
 */
public class MemoryBasedDataStructureWithDsBaseAddress extends MemoryBasedDataStructureWithSegmentRegisterBaseAddress {
  public MemoryBasedDataStructureWithDsBaseAddress(Machine machine) {
    super(machine, SegmentRegisters.DS_INDEX);
  }
}
