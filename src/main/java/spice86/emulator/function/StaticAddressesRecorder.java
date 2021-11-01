package spice86.emulator.function;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import spice86.emulator.cpu.SegmentRegisters;
import spice86.emulator.cpu.State;

/**
 * Records the addresses accessed in memory and what is done there.
 */
public class StaticAddressesRecorder {
  private boolean debugMode;
  private SegmentRegisters segmentRegisters;
  private Map<SegmentRegisterBasedAddress, SegmentRegisterBasedAddress> segmentRegisterBasedAddress = new HashMap<>();
  private SegmentRegisterBasedAddress currentValue;
  private AddressOperation currentAddressOperation;

  public StaticAddressesRecorder(State state, boolean debugMode) {
    this.debugMode = debugMode;
    this.segmentRegisters = state.getSegmentRegisters();
  }

  public void reset() {
    currentValue = null;
    currentAddressOperation = null;
  }

  public void commit() {
    if (debugMode && currentValue != null && currentAddressOperation != null) {
      SegmentRegisterBasedAddress value = segmentRegisterBasedAddress.computeIfAbsent(currentValue, k -> k);
      value.addAddressOperation(currentAddressOperation);
      value.addSegmentValue(segmentRegisters.getRegister(value.getRegisterIndex()));
    }
  }

  public void addOffset(int regIndex, int offset) {
    currentValue = new SegmentRegisterBasedAddress(regIndex, offset);
  }

  public void setCurrentAddressOperation(ValueOperation valueOperation, OperandSize operandSize) {
    currentAddressOperation = new AddressOperation(valueOperation, operandSize);
  }

  public Collection<SegmentRegisterBasedAddress> getSegmentRegisterBasedAddress() {
    return segmentRegisterBasedAddress.values();
  }
}
