package spice86.emulator.devices.timer;

/**
 * Counter activator based on real system time
 */
public class TimeCounterActivator implements CounterActivator {
  private double multiplier;
  private long timeBetweenTicks;
  private long lastActivationTime = System.nanoTime();

  public TimeCounterActivator(double multiplier) {
    this.multiplier = multiplier;
  }

  @Override
  public boolean isActivated() {
    long currentTime = System.nanoTime();
    long elapsedTime = currentTime - lastActivationTime;
    if (elapsedTime <= timeBetweenTicks) {
      return false;
    }
    lastActivationTime = currentTime;
    return true;
  }

  @Override
  public void updateDesiredFreqency(long desiredFrequency) {
    timeBetweenTicks = (long)(1_000_000_000 / (multiplier *desiredFrequency));
  }
}
