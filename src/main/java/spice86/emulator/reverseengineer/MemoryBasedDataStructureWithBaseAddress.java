package spice86.emulator.reverseengineer;

import spice86.emulator.memory.Memory;

/**
 * Memory based data structure with fixed base address.
 */
public class MemoryBasedDataStructureWithBaseAddress extends MemoryBasedDataStructureWithBaseAddressProvider {
  private int baseAddress;

  public MemoryBasedDataStructureWithBaseAddress(Memory memory, int baseAddress) {
    super(memory);
    this.baseAddress = baseAddress;
  }

  @Override
  public int getBaseAddress() {
    return baseAddress;
  }

}
