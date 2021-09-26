package spice86.emulator.devices.timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.devices.externalinterrupt.Pic;
import spice86.emulator.devices.video.VgaCard;
import spice86.emulator.errors.InvalidOperationException;
import spice86.emulator.ioports.DefaultIOPortHandler;
import spice86.emulator.ioports.IOPortDispatcher;
import spice86.emulator.machine.Machine;

/**
 * Emulates a PIT8254 Programmable Interval Timer.<br/>
 * As a shortcut also triggers screen refreshes 60 times per second.<br/>
 * Triggers interrupt 8 on the CPU via the PIC.<br/>
 * https://k.lse.epita.fr/internals/8254_controller.html
 */
public class Timer extends DefaultIOPortHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(Timer.class);
  private static final int COUNTER_REGISTER_0 = 0x40;
  private static final int COUNTER_REGISTER_1 = 0x41;
  private static final int COUNTER_REGISTER_2 = 0x42;
  private static final int MODE_COMMAND_REGISTER = 0x43;

  private Counter[] counters = new Counter[3];
  private Pic pic;

  // Cheat: display at 60fps
  private Counter vgaCounter;
  private VgaCard vgaCard;

  public Timer(Machine machine, Pic pic, VgaCard vgaCard,
      long instructionsPerSecond, boolean failOnUnhandledPort) {
    super(machine, failOnUnhandledPort);
    this.pic = pic;
    this.vgaCard = vgaCard;
    this.cpu = machine.getCpu();
    for (int i = 0; i < counters.length; i++) {
      counters[i] = new Counter(machine, i, instructionsPerSecond);
    }
    vgaCounter = new Counter(machine, 4, instructionsPerSecond);
    // 60fps
    vgaCounter.setValue((int)(Counter.HARDWARE_FREQUENCY / 60));
  }

  public long getNumberOfTicks() {
    return counters[0].getTicks();
  }

  public void tick() {
    long cycles = cpu.getState().getCycles();
    if (counters[0].processActivation(cycles)) {
      pic.processInterrupt(0x8);
    }
    if (vgaCounter.processActivation(cycles)) {
      vgaCard.updateScreen();
    }
  }

  @Override
  public void initPortHandlers(IOPortDispatcher ioPortDispatcher) {
    ioPortDispatcher.addIOPortHandler(MODE_COMMAND_REGISTER, this);
    ioPortDispatcher.addIOPortHandler(COUNTER_REGISTER_0, this);
    ioPortDispatcher.addIOPortHandler(COUNTER_REGISTER_1, this);
    ioPortDispatcher.addIOPortHandler(COUNTER_REGISTER_2, this);
  }

  @Override
  public int inb(int port) throws InvalidOperationException {
    if (isCounterRegisterPort(port)) {
      Counter counter = getCounterIndexFromPortNumber(port);
      int value = counter.getValueUsingMode();
      LOGGER.info("READING COUNTER {}, partial value is {}", counter, value);
      return value;
    }
    return super.inb(port);
  }

  private boolean isCounterRegisterPort(int port) {
    return port >= COUNTER_REGISTER_0 && port <= COUNTER_REGISTER_2;
  }

  @Override
  public void outb(int port, int value) throws InvalidOperationException {
    if (isCounterRegisterPort(port)) {
      Counter counter = getCounterIndexFromPortNumber(port);
      LOGGER.info("SETTING COUNTER {} to partial value {}", counter.getIndex(), value);
      counter.setValueUsingMode(value);
      return;
    } else if (port == MODE_COMMAND_REGISTER) {
      int counterIndex = (value >> 6);
      Counter counter = getCounter(counterIndex);
      counter.setReadWritePolicy((value >> 4) & 0b11);
      counter.setMode((value >> 1) & 0b111);
      counter.setBcd(value & 1);
      LOGGER.info("SETTING CONTROL REGISTER FOR COUNTER {}. {}", counterIndex, counter);
      return;
    }
    super.outb(port, value);
  }

  private Counter getCounter(int counterIndex) throws InvalidCounterIndexException {
    if (counterIndex > counters.length || counterIndex < 0) {
      throw new InvalidCounterIndexException(machine, counterIndex);
    }
    return counters[counterIndex];
  }

  private Counter getCounterIndexFromPortNumber(int port) throws InvalidCounterIndexException {
    int counter = port & 0b11;
    return getCounter(counter);
  }
}
