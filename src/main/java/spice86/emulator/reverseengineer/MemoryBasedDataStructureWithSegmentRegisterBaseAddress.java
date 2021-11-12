package spice86.emulator.reverseengineer;

import spice86.emulator.cpu.SegmentRegisters;
import spice86.emulator.machine.Machine;

public class MemoryBasedDataStructureWithSegmentRegisterBaseAddress
    extends MemoryBasedDataStructureWithBaseAddressProvider {
  private SegmentRegisters segmentRegisters;
  private int segmentRegisterIndex;

  public MemoryBasedDataStructureWithSegmentRegisterBaseAddress(Machine machine, int segmentRegisterIndex) {
    super(machine.getMemory());
    this.segmentRegisterIndex = segmentRegisterIndex;
    this.segmentRegisters = machine.getCpu().getState().getSegmentRegisters();
  }

  @Override
  public int getBaseAddress() {
    return segmentRegisters.getRegister(segmentRegisterIndex) * 0x10;
  }

}
