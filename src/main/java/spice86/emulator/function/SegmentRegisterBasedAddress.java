package spice86.emulator.function;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import spice86.emulator.memory.SegmentedAddress;

/**
 * Represents a memory address with a register for the segment and a fixed offset.<br/>
 * Lists how this address is accessed.
 */
public class SegmentRegisterBasedAddress extends SegmentedAddress {
  private String name;
  // Address operations association to list of segments for which it was done
  private Map<AddressOperation, Set<Integer>> addressOperations = new HashMap<>();

  public SegmentRegisterBasedAddress(int segment, int offset, String name) {
    super(segment, offset);
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public Map<AddressOperation, Set<Integer>> getAddressOperations() {
    return addressOperations;
  }

  public void addAddressOperation(AddressOperation addressOperation, int segmentRegisterIndex) {
    Set<Integer> segmentRegisterIndexes = addressOperations.computeIfAbsent(addressOperation, k -> new HashSet<>());
    segmentRegisterIndexes.add(segmentRegisterIndex);
  }
}
