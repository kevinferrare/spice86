package spice86.emulator.reverseengineer;

import spice86.emulator.cpu.SegmentRegisters;
import spice86.emulator.machine.Machine;

/**
 * memory based data structure where the base address is the FS segment
 */
public class MemoryBasedDataStructureWithFsBaseAddress extends MemoryBasedDataStructureWithSegmentRegisterBaseAddress {
  public MemoryBasedDataStructureWithFsBaseAddress(Machine machine) {
    super(machine, SegmentRegisters.FS_INDEX);
  }
}
