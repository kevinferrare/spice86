package spice86.emulator.function;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import spice86.emulator.errors.InvalidOperationException;
import spice86.emulator.memory.SegmentedAddress;
import spice86.utils.CheckedSupplier;
import spice86.utils.ConvertUtils;

/**
 * Represents the informations about a function.<br/>
 * Those are dynamically collected and enriched at runtime.<br/>
 * It is possible to provide a FunctionInformation with an override. In that case:
 * <ul>
 * <li>Supplier field override is called instead of the assembly code of the function when it is called via call / int
 * instructions.</li>
 * <li>The Runnable provided by the Supplier is executed when it is done. This is useful for post-execution actions that
 * need to be done only in VM world like re-arranging the stack for returns.</li>
 * </ul>
 */
public class FunctionInformation implements Comparable<FunctionInformation> {
  private SegmentedAddress address;
  private String name;
  // Addresses and types of the return instructions encountered
  private Map<FunctionReturn, Set<SegmentedAddress>> returns = new HashMap<>();
  // Addresses and types of the return instructions encountered that did not result in an actual return due to stack not
  // being in the state it was at function entry.
  private Map<FunctionReturn, Set<SegmentedAddress>> unalignedReturns = new HashMap<>();
  // Functions that calls this one
  private Set<FunctionInformation> callers = new HashSet<>();
  // Override to execute instead of the assembly
  private CheckedSupplier<Runnable, InvalidOperationException> override;
  // Number of times this function was called
  private int calledCount;

  public FunctionInformation(SegmentedAddress address, String name) {
    this(address, name, null);
  }

  public FunctionInformation(SegmentedAddress address, String name,
      CheckedSupplier<Runnable, InvalidOperationException> override) {
    this.address = address;
    this.name = name;
    this.override = override;
  }

  public void enter(FunctionInformation caller) {
    if (caller != null) {
      this.callers.add(caller);
    }
    calledCount++;
  }

  public int getCalledCount() {
    return calledCount;
  }

  public boolean hasOverride() {
    return override != null;
  }

  public void callOverride() throws InvalidOperationException {
    if (hasOverride()) {
      Runnable retHandler = override.get();
      // usually retHandler is a ret when called with a near call / retf when called with a far call / iref when called
      // via int.
      // but in some cases, for some crazy reasons, functions dont do this and the caller handles the stack manually.
      // let the user define what type of ret to do.
      retHandler.run();
    }
  }

  public void addUnalignedReturn(FunctionReturn functionReturn, SegmentedAddress target) {
    addReturn(unalignedReturns, functionReturn, target);
  }

  public void addReturn(FunctionReturn functionReturn, SegmentedAddress target) {
    addReturn(returns, functionReturn, target);
  }

  private void addReturn(Map<FunctionReturn, Set<SegmentedAddress>> returnsMap, FunctionReturn functionReturn,
      SegmentedAddress target) {
    Set<SegmentedAddress> addresses = returnsMap.computeIfAbsent(functionReturn, f -> new HashSet<>());
    if (target != null) {
      addresses.add(target);
    }
  }

  public String getName() {
    return name;
  }

  public SegmentedAddress getAddress() {
    return address;
  }

  public Map<FunctionReturn, Set<SegmentedAddress>> getReturns() {
    return returns;
  }

  public Map<FunctionReturn, Set<SegmentedAddress>> getUnalignedReturns() {
    return unalignedReturns;
  }

  public Set<FunctionInformation> getCallers() {
    return callers;
  }

  public String generateName() {
    String res = name + "_" + ConvertUtils.toHex(address.getSegment()) + "_" + ConvertUtils.toHex(address.getOffset())
        + "_" + ConvertUtils.toHex(address.toPhysical());
    if (override != null) {
      res += " overriden";
    }
    return res;
  }

  @Override
  public int hashCode() {
    return address.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    return (obj instanceof FunctionInformation other) && address.equals(other.address);
  }

  @Override
  public int compareTo(FunctionInformation o) {
    return this.getAddress().compareTo(o.getAddress());
  }

  @Override
  public String toString() {
    return generateName();
  }
}
