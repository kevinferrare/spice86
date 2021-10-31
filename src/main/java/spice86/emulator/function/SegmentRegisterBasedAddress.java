package spice86.emulator.function;

import java.util.HashSet;
import java.util.Set;

import spice86.emulator.cpu.SegmentRegisters;
import spice86.utils.ConvertUtils;

/**
 * Represents a memory address with a register for the segment and a fixed offset.<br/>
 * Lists how this address is accessed.
 */
public class SegmentRegisterBasedAddress implements Comparable<SegmentRegisterBasedAddress> {
  private static final SegmentRegisters SEGMENT_REGISTERS = new SegmentRegisters();
  private int registerIndex;
  private int offset;
  private Set<Integer> segmentValues;
  private Set<AddressOperation> addressOperations;

  public SegmentRegisterBasedAddress(int registerIndex, int offset) {
    this.registerIndex = registerIndex;
    this.offset = offset;
  }

  public int getRegisterIndex() {
    return registerIndex;
  }

  public int getOffset() {
    return offset;
  }

  public Set<Integer> getSegmentValues() {
    return segmentValues;
  }

  public void addSegmentValue(int segment) {
    if (segmentValues == null) {
      segmentValues = new HashSet<>();
    }
    segmentValues.add(segment);
  }

  public Set<AddressOperation> getAddressOperations() {
    return addressOperations;
  }

  public void addAddressOperation(AddressOperation addressOperation) {
    if (addressOperations == null) {
      addressOperations = new HashSet<>();
    }
    addressOperations.add(addressOperation);
  }

  @Override
  public int hashCode() {
    // Avoid collisions and keep natural ordering by shifting the register index above the offset 16 bits
    return registerIndex << 16 | offset;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    return (obj instanceof SegmentRegisterBasedAddress other) && offset == other.offset
        && registerIndex == other.registerIndex;
  }

  public String getRegisterName() {
    return SEGMENT_REGISTERS.getRegName(registerIndex);
  }

  public String getOffsetHexString() {
    return ConvertUtils.toHex16WithoutX(offset);
  }

  @Override
  public String toString() {
    return getRegisterName() + ':' + getOffsetHexString();
  }

  @Override
  public int compareTo(SegmentRegisterBasedAddress other) {
    return Integer.compare(this.hashCode(), other.hashCode());
  }
}
