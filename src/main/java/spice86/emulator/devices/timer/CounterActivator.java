package spice86.emulator.devices.timer;

/**
 * Common interface to control a timer activation
 */
public interface CounterActivator {
  /**
   * @return true when activation can occurr. If called twice in a row, next call can return false.
   */
  public boolean isActivated();

  /**
   * @param desiredFrequency
   *          the activation frequency
   */
  public void updateDesiredFreqency(long desiredFrequency);
}
