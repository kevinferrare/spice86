package spice86.emulator.machine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.errors.UnrecoverableException;

/**
 * Handles pause / resume from another thread and ensures the thread is paused in waitIfPaused.<br/>
 */
public class PauseHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(PauseHandler.class);

  private volatile boolean pauseRequested;
  private volatile boolean paused;
  private volatile boolean pauseEnded;

  public void requestPause() {
    pauseRequested = true;
    logStatus("requestPause finished");
  }

  public void requestPauseAndWait() {
    logStatus("requestPauseAndWait started");
    pauseRequested = true;
    while (!paused)
      ;
    logStatus("requestPauseAndWait finished");
  }

  public void requestResume() {
    logStatus("requestResume started");
    pauseRequested = false;
    synchronized (this) {
      this.notifyAll();
    }
    logStatus("requestResume finished");
  }

  public void waitIfPaused() {
    while (pauseRequested) {
      logStatus("waitIfPaused will wait");
      paused = true;
      await();
      logStatus("waitIfPaused awoke");
    }
    paused = false;
    pauseEnded = true;
  }

  // This is called from a loop
  @SuppressWarnings("java:S2274")
  private void await() {
    try {
      synchronized (this) {
        this.wait();
      }
    } catch (InterruptedException exception) {
      Thread.currentThread()
          .interrupt();
      throw new UnrecoverableException("Fatal error while waiting paused", exception);
    }
  }

  private void logStatus(String message) {
    LOGGER.debug("{}: pauseRequested:{},paused:{},pauseEnded:{}", message, pauseRequested, paused, pauseEnded);
  }
}
