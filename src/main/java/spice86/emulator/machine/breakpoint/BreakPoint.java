package spice86.emulator.machine.breakpoint;

import java.util.function.Consumer;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Represents a breakpoint in the Machine.<br/>
 * Breakpoints concern different components of the machine depending on their breakPointType property.<br/>
 * Code to execute when the condition of the breakpoint is triggered can be specified in the onReached field.<br/>
 * If removeOnTrigger is true, the breakpoint is removed after it is triggered.
 */
public class BreakPoint {
  private BreakPointType breakPointType;
  private long address;
  private Consumer<BreakPoint> onReached;
  private boolean removeOnTrigger;

  public BreakPoint(BreakPointType breakPointType, long address, Consumer<BreakPoint> onReached,
      boolean removeOnTrigger) {
    this.breakPointType = breakPointType;
    this.address = address;
    this.onReached = onReached;
    this.removeOnTrigger = removeOnTrigger;
  }

  public BreakPointType getBreakPointType() {
    return breakPointType;
  }

  public long getAddress() {
    return address;
  }

  public boolean isRemoveOnTrigger() {
    return removeOnTrigger;
  }

  public boolean matches(long address) {
    return this.address == address;
  }

  public boolean matches(long startAddress, long endAddress) {
    return this.address >= startAddress && this.address < endAddress;
  }

  public void trigger() {
    onReached.accept(this);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(breakPointType).append(address).toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    return (obj instanceof BreakPoint other)
        && address == other.address
        && breakPointType == other.breakPointType;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.JSON_STYLE)
        .append("breakPointType", breakPointType)
        .append("address", address)
        .append("removeOnTrigger", removeOnTrigger)
        .toString();
  }

}
