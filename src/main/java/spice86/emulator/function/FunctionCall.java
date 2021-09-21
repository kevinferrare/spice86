package spice86.emulator.function;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import spice86.emulator.memory.SegmentedAddress;

/**
 * Represents the state of the machine when a function is called.<br/>
 * Functions can be called from the VM (near / far / interrupt) or at the start of the machine for entry point
 */
public class FunctionCall {
  private CallType callType;
  private SegmentedAddress entryPointAddress;
  private SegmentedAddress expectedReturnAddress;
  // stores the status of the stack after the function returns have been pushed to the stack so that it can be examined
  // if expected return differs from actual return.
  private SegmentedAddress stackAddressAfterCall;
  private boolean recordReturn;

  public FunctionCall(CallType callType, SegmentedAddress entryPointAddress, SegmentedAddress expectedReturnAddress,
      SegmentedAddress stackAddressAfterCall, boolean recordReturn) {
    super();
    this.callType = callType;
    this.entryPointAddress = entryPointAddress;
    this.expectedReturnAddress = expectedReturnAddress;
    this.stackAddressAfterCall = stackAddressAfterCall;
    this.recordReturn = recordReturn;
  }

  public CallType getCallType() {
    return callType;
  }

  public SegmentedAddress getEntryPointAddress() {
    return entryPointAddress;
  }

  public SegmentedAddress getExpectedReturnAddress() {
    return expectedReturnAddress;
  }

  public SegmentedAddress getStackAddressAfterCall() {
    return stackAddressAfterCall;
  }

  public boolean isRecordReturn() {
    return recordReturn;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.JSON_STYLE).toString();
  }
}
