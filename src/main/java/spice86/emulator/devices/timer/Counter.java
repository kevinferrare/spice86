package spice86.emulator.devices.timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import spice86.emulator.errors.UnhandledOperationException;
import spice86.emulator.machine.Machine;
import spice86.utils.ConvertUtils;

/**
 * Counter of the PIT.<br/>
 * Time is counted with CPU cycles so that the program does get interruptions at fixed and predictable points.<br/>
 * Number of CPU cycles to consider one second elapsed is defined in instructionsPerSecond.<br/>
 * Some documentation: https://k.lse.epita.fr/data/8254.pdf
 */
public class Counter {
  private static final Logger LOGGER = LoggerFactory.getLogger(Counter.class);

  public static final long HARDWARE_FREQUENCY = 1_193_182;
  @SuppressWarnings({
      // Using transient to prevent GSon from serializing it
      "java:S2065" })
  private transient Machine machine;
  private int index;
  // Some programs don't set it so let's use by default the simplest mode
  private int readWritePolicy = 1;
  private int mode;
  private int bcd;
  private int value;
  private boolean firstByteRead;
  private boolean firstByteWritten;
  private long ticks;
  private CounterActivator activator;

  public Counter(Machine machine, int index, CounterActivator activator) {
    this.machine = machine;
    this.index = index;
    this.activator = activator;
    // Default is 18.2 times per second
    updateDesiredFreqency(18);
  }

  public int getIndex() {
    return index;
  }

  public int getReadWritePolicy() {
    return readWritePolicy;
  }

  public void setReadWritePolicy(int readWritePolicy) {
    this.readWritePolicy = readWritePolicy;
  }

  public int getMode() {
    return mode;
  }

  public void setMode(int mode) {
    this.mode = mode;
  }

  public int getBcd() {
    return bcd;
  }

  public void setBcd(int bcd) {
    this.bcd = bcd;
  }

  public int getValue() {
    return value;
  }

  public void setValue(int counter) {
    this.value = counter;
    onValueWrite();
  }

  public long getTicks() {
    return ticks;
  }

  public int getValueUsingMode() throws UnhandledOperationException {
    return switch (readWritePolicy) {
      case 0 -> throw new UnhandledOperationException(machine,
          "Latch read is not implemented yet");
      case 1 -> readLsb();
      case 2 -> readMsb();
      case 3 -> readPolicy3();
      default -> throw new UnhandledOperationException(machine, "Invalid readWritePolicy " + readWritePolicy);
    };
  }

  public void setValueUsingMode(int partialValue) throws UnhandledOperationException {
    switch (readWritePolicy) {
      case 1 -> writeLsb(partialValue);
      case 2 -> writeMsb(partialValue);
      case 3 -> writePolicy3(partialValue);
      default -> throw new UnhandledOperationException(machine, "Invalid readWritePolicy " + readWritePolicy);
    }
    onValueWrite();
  }

  private int readPolicy3() {
    // LSB first, then MSB
    if (firstByteRead) {
      // return msb
      firstByteRead = false;
      return readMsb();
    }
    // else return lsb
    firstByteRead = true;
    return readLsb();
  }

  private void writePolicy3(int partialValue) {
    // LSB first, then MSB
    if (firstByteWritten) {
      // write msb
      firstByteWritten = false;
      writeMsb(partialValue);
      // Fully written
    } else {
      // else write lsb
      firstByteWritten = true;
      writeLsb(partialValue);
    }
  }

  private int readLsb() {
    return ConvertUtils.readLsb(value);
  }

  private void writeLsb(int partialValue) {
    value = ConvertUtils.writeLsb(value, partialValue);
  }

  private int readMsb() {
    return ConvertUtils.readMsb(value);
  }

  private void writeMsb(int partialValue) {
    value = ConvertUtils.writeMsb(value, partialValue);
  }

  private void onValueWrite() {
    if (value == 0) {
      updateDesiredFreqency(HARDWARE_FREQUENCY / 0x10000);
    } else {
      updateDesiredFreqency(HARDWARE_FREQUENCY / value);
    }
  }

  private void updateDesiredFreqency(long desiredFrequency) {
    activator.updateDesiredFreqency(desiredFrequency);
    LOGGER.info("Updating counter {} frequency to {}.", index, desiredFrequency);
  }

  /**
   * @param currentCycles
   *          current cycle count of the CPU
   * @return true if latched, false otherwise
   */
  public boolean processActivation(long currentCycles) {
    if (activator.isActivated()) {
      ticks++;
      return true;
    }
    return false;
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }
}
