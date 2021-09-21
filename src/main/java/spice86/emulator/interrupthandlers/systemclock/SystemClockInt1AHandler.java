package spice86.emulator.interrupthandlers.systemclock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.errors.UnhandledOperationException;
import spice86.emulator.interrupthandlers.InterruptHandler;
import spice86.emulator.interrupthandlers.timer.TimerInt8Handler;
import spice86.emulator.machine.Machine;

/**
 * Implementation of int1A.
 */
public class SystemClockInt1AHandler extends InterruptHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(SystemClockInt1AHandler.class);

  private TimerInt8Handler timerHandler;

  public SystemClockInt1AHandler(Machine machine, TimerInt8Handler timerHandler) {
    super(machine);
    this.timerHandler = timerHandler;
    super.dispatchTable.put(0x00, this::setSystemClockCounter);
    super.dispatchTable.put(0x01, this::getSystemClockCounter);
    super.dispatchTable.put(0x81, this::tandySoundSystemUnhandled);
    super.dispatchTable.put(0x82, this::tandySoundSystemUnhandled);
    super.dispatchTable.put(0x83, this::tandySoundSystemUnhandled);
    super.dispatchTable.put(0x84, this::tandySoundSystemUnhandled);
    super.dispatchTable.put(0x85, this::tandySoundSystemUnhandled);
  }

  @Override
  public int getIndex() {
    return 0x1A;
  }

  @Override
  public void run() throws UnhandledOperationException {
    int operation = state.getAH();
    this.run(operation);
  }

  public void setSystemClockCounter() {
    int value = (state.getCX() << 16) | state.getDX();
    LOGGER.info("SET SYSTEM CLOCK COUNTER value={}", value);
    timerHandler.setTickCounterValue(value);
  }

  public void getSystemClockCounter() {
    int value = timerHandler.getTickCounterValue();
    LOGGER.info("GET SYSTEM CLOCK COUNTER value={}", value);
    // let's say it never overflows
    state.setAL(0);
    state.setCX(value >>> 16);
    state.setDX(value);
  }

  private void tandySoundSystemUnhandled() {
    LOGGER.info("TANDY SOUND SYSTEM IS NOT IMPLEMENTED");
  }
}
