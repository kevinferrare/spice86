package spice86.emulator.function;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import spice86.emulator.cpu.SegmentRegisters;
import spice86.emulator.cpu.State;
import spice86.emulator.memory.MemoryUtils;
import spice86.emulator.memory.SegmentedAddress;

/**
 * Records the addresses accessed in memory and what is done there.
 */
public class StaticAddressesRecorder {
  private boolean debugMode;
  private SegmentRegisters segmentRegisters;
  private Set<SegmentedAddress> whiteListOfSegmentForOffset = new HashSet<>();
  private Map<Integer, SegmentRegisterBasedAddress> segmentRegisterBasedAddress = new HashMap<>();
  private Map<Integer, String> names = new HashMap<>();
  private Integer currentSegmentIndex;
  private Integer currentOffset;
  private AddressOperation currentAddressOperation;

  public StaticAddressesRecorder(State state, boolean debugMode) {
    this.debugMode = debugMode;
    this.segmentRegisters = state.getSegmentRegisters();
  }

  public Map<Integer, String> getNames() {
    return names;
  }

  public void addName(int physicalAddress, String name) {
    names.put(physicalAddress, name);
  }

  public void reset() {
    currentSegmentIndex = null;
    currentOffset = null;
    currentAddressOperation = null;
  }

  public void commit() {
    if (debugMode && currentSegmentIndex != null && currentOffset != null && currentAddressOperation != null) {
      int segmentValue = segmentRegisters.getRegister(currentSegmentIndex);
      int physicalAddress = MemoryUtils.toPhysicalAddress(segmentValue, currentOffset);
      SegmentRegisterBasedAddress value = segmentRegisterBasedAddress.computeIfAbsent(physicalAddress,
          a -> new SegmentRegisterBasedAddress(segmentValue, currentOffset, names.get(physicalAddress)));
      value.addAddressOperation(currentAddressOperation, currentSegmentIndex);
    }
  }

  public void setCurrentValue(int regIndex, int offset) {
    currentSegmentIndex = regIndex;
    currentOffset = offset;
  }

  public void setCurrentAddressOperation(ValueOperation valueOperation, OperandSize operandSize) {
    currentAddressOperation = new AddressOperation(valueOperation, operandSize);
  }

  public Collection<SegmentRegisterBasedAddress> getSegmentRegisterBasedAddress() {
    return segmentRegisterBasedAddress.values();
  }

  public Set<SegmentedAddress> getWhiteListOfSegmentForOffset() {
    return whiteListOfSegmentForOffset;
  }

  public void addSegmentTowhiteList(SegmentedAddress address) {
    whiteListOfSegmentForOffset.add(address);
  }
}
