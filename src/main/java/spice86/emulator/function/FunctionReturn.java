package spice86.emulator.function;

import spice86.emulator.memory.SegmentedAddress;

/**
 * Represents a return instruction encountered by a function.<br/>
 * Contains the address of the instruction the type of return executed.
 */
public class FunctionReturn implements Comparable<FunctionReturn> {
  private CallType returnCallType;
  private SegmentedAddress instructionAddress;

  public FunctionReturn(CallType returnCallType, SegmentedAddress instructionAddress) {
    this.returnCallType = returnCallType;
    this.instructionAddress = instructionAddress;
  }

  public CallType getReturnCallType() {
    return returnCallType;
  }

  public SegmentedAddress getAddress() {
    return instructionAddress;
  }

  @Override
  public int hashCode() {
    return instructionAddress.hashCode() + returnCallType.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return (obj instanceof FunctionReturn other)
        && this.instructionAddress.equals(other.instructionAddress)
        && this.returnCallType.equals(other.returnCallType);
  }

  @Override
  public int compareTo(FunctionReturn o) {
    return this.instructionAddress.compareTo(o.instructionAddress);
  }

  @Override
  public String toString() {
    return this.returnCallType.name() + " at " + this.instructionAddress.toString();
  }
}
