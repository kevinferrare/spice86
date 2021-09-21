package spice86.emulator.machine.breakpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Breakpoint container.<br/>
 * Can toggle them and trigger them.<br/>
 * There can be several breakpoints per address as breakpoints are not only used for debugging.
 */
public class BreakPointHolder {
  private List<BreakPoint> unconditionalBreakPoints = new ArrayList<>();
  private Map<Long, List<BreakPoint>> breakPoints = new HashMap<>();

  public boolean isEmpty() {
    return breakPoints.isEmpty() && unconditionalBreakPoints.isEmpty();
  }

  public void toggleBreakPoint(BreakPoint breakPoint, boolean on) {
    if (breakPoint instanceof UnconditionalBreakPoint) {
      toggleUnconditionalBreakPointBreakPoint(breakPoint, on);
    } else {
      toggleConditionalBreakPoint(breakPoint, on);
    }
  }

  private void toggleUnconditionalBreakPointBreakPoint(BreakPoint breakPoint, boolean on) {
    if (on) {
      unconditionalBreakPoints.add(breakPoint);
    } else {
      unconditionalBreakPoints.remove(breakPoint);
    }
  }

  private void toggleConditionalBreakPoint(BreakPoint breakPoint, boolean on) {
    Long address = breakPoint.getAddress();
    if (on) {
      List<BreakPoint> breakPointList =
          breakPoints.computeIfAbsent(address, key -> new ArrayList<>());
      breakPointList.add(breakPoint);
    } else {
      List<BreakPoint> breakPointList = breakPoints.get(address);
      if (breakPointList != null) {
        breakPointList.remove(breakPoint);
        if (breakPointList.isEmpty()) {
          breakPoints.remove(address);
        }
      }
    }
  }

  public void triggerMatchingBreakPoints(long address) {
    if (!breakPoints.isEmpty()) {
      List<BreakPoint> breakPointList = breakPoints.get(address);
      if (breakPointList != null) {
        triggerBreakPointsFromList(breakPointList, address);
        if (breakPointList.isEmpty()) {
          breakPoints.remove(address);
        }
      }
    }
    if (!unconditionalBreakPoints.isEmpty()) {
      triggerBreakPointsFromList(unconditionalBreakPoints, address);
    }
  }

  private void triggerBreakPointsFromList(List<BreakPoint> breakPointList, long address) {
    Iterator<BreakPoint> it = breakPointList.iterator();
    while (it.hasNext()) {
      BreakPoint breakPoint = it.next();
      if (breakPoint.matches(address)) {
        breakPoint.trigger();
        if (breakPoint.isRemoveOnTrigger()) {
          it.remove();
        }
      }
    }
  }

  public void triggerBreakPointsWithAddressRange(long startAddress, long endAddress) {
    if (!breakPoints.isEmpty()) {
      for (List<BreakPoint> breakPointList : breakPoints.values()) {
        triggerBreakPointsWithAddressRangeFromList(breakPointList, startAddress, endAddress);
      }
    }
    if (!unconditionalBreakPoints.isEmpty()) {
      triggerBreakPointsWithAddressRangeFromList(unconditionalBreakPoints, startAddress, endAddress);
    }
  }

  private void triggerBreakPointsWithAddressRangeFromList(List<BreakPoint> breakPointList, long startAddress,
      long endAddress) {
    for (BreakPoint breakPoint : breakPointList) {
      if (breakPoint.matches(startAddress, endAddress)) {
        breakPoint.trigger();
      }
    }
  }
}
