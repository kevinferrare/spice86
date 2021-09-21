package spice86.emulator.machine;

import spice86.emulator.cpu.State;
import spice86.emulator.machine.breakpoint.BreakPoint;
import spice86.emulator.machine.breakpoint.BreakPointHolder;
import spice86.emulator.machine.breakpoint.BreakPointType;
import spice86.emulator.memory.Memory;

/**
 * Handler for breakpoints. Route them to the correct subsystem if needed.
 */
public class MachineBreakpoints {
  private State state;
  private Memory memory;

  private PauseHandler pauseHandler = new PauseHandler();
  private BreakPoint machineStopBreakPoint;
  private BreakPointHolder executionBreakPoints = new BreakPointHolder();
  private BreakPointHolder cycleBreakPoints = new BreakPointHolder();

  public MachineBreakpoints(Machine machine) {
    this.state = machine.getCpu().getState();
    this.memory = machine.getMemory();
  }

  public void toggleBreakPoint(BreakPoint breakPoint, boolean on) {
    BreakPointType breakPointType = breakPoint.getBreakPointType();
    if (breakPointType.equals(BreakPointType.EXECUTION)) {
      executionBreakPoints.toggleBreakPoint(breakPoint, on);
    } else if (breakPointType.equals(BreakPointType.CYCLES)) {
      cycleBreakPoints.toggleBreakPoint(breakPoint, on);
    } else if (breakPointType.equals(BreakPointType.MACHINE_STOP)) {
      machineStopBreakPoint = breakPoint;
    } else {
      memory.toggleBreakPoint(breakPoint, on);
    }
  }

  private void checkBreakPoints() {
    if (!executionBreakPoints.isEmpty()) {
      int address = state.getIpPhysicalAddress();
      executionBreakPoints.triggerMatchingBreakPoints(address);
    }
    if (!cycleBreakPoints.isEmpty()) {
      long cycles = state.getCycles();
      cycleBreakPoints.triggerMatchingBreakPoints(cycles);
    }
  }

  public void checkBreakPoint() {
    checkBreakPoints();
    pauseHandler.waitIfPaused();
  }

  public void onMachineStop() {
    if (machineStopBreakPoint != null) {
      machineStopBreakPoint.trigger();
      pauseHandler.waitIfPaused();
    }
  }

  public PauseHandler getPauseHandler() {
    return pauseHandler;
  }
}
