package spice86.emulator.devices.timer;

import spice86.emulator.cpu.State;

/**
 * Counter activator based on emulated cycles
 */
public class CyclesCounterActivator implements CounterActivator {
  private State state;
  private long lastActivationCycle;
  private long cyclesBetweenActivations;
  private long instructionsPerSecond;

  public CyclesCounterActivator(State state, long instructionsPerSecond) {
    this.state = state;
    this.instructionsPerSecond = instructionsPerSecond;
  }

  @Override
  public boolean isActivated() {
    long currentCycles = state.getCycles();
    long elapsedInstructions = state.getCycles() - lastActivationCycle;
    if (elapsedInstructions <= cyclesBetweenActivations) {
      return false;
    }
    lastActivationCycle = currentCycles;
    return true;
  }

  @Override
  public void updateDesiredFreqency(long desiredFrequency) {
    cyclesBetweenActivations = this.instructionsPerSecond / desiredFrequency;
  }

}
