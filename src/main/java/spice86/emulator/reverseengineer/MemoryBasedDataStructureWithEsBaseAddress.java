package spice86.emulator.reverseengineer;

import spice86.emulator.cpu.SegmentRegisters;
import spice86.emulator.machine.Machine;

/**
 * memory based data structure where the base address is the ES segment
 */
public class MemoryBasedDataStructureWithEsBaseAddress extends MemoryBasedDataStructureWithSegmentRegisterBaseAddress {
  public MemoryBasedDataStructureWithEsBaseAddress(Machine machine) {
    super(machine, SegmentRegisters.ES_INDEX);
  }
}
