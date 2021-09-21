package spice86.emulator.machine.breakpoint;

import java.util.function.Consumer;

/**
 * Breakpoint that is triggered without any condition, the next time the system target by breakPointType checks.
 */
public class UnconditionalBreakPoint extends BreakPoint {

  public UnconditionalBreakPoint(BreakPointType breakPointType, Consumer<BreakPoint> onReached,
      boolean removeOnTrigger) {
    super(breakPointType, 0, onReached, removeOnTrigger);
  }

  @Override
  public boolean matches(long address) {
    return true;
  }

  @Override
  public boolean matches(long startAddress, long endAddress) {
    return true;
  }
}
