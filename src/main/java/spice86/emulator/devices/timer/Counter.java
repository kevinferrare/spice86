package spice86.emulator.devices.timer;

import static spice86.utils.ConvertUtils.uint8;

import com.google.gson.Gson;

import spice86.emulator.errors.UnhandledOperationException;
import spice86.emulator.machine.Machine;

/**
 * Counter of the PIT.<br/>
 * Time is counted with CPU cycles so that the program does get interruptions at fixed and predictable points.<br/>
 * Number of CPU cycles to consider one second elapsed is defined in instructionsPerSecond.<br/>
 * Some documentation: https://k.lse.epita.fr/data/8254.pdf
 */
public class Counter {
  public static final long HARDWARE_FREQUENCY = 1_193_182;
  private Machine machine;
  private int index;
  // Some programs don't set it so let's use by default the simplest mode
  private int readWritePolicy = 1;
  private int mode;
  private int bcd;
  private int value;
  private boolean firstByteRead;
  private boolean firstByteWritten;
  private long lastActivationCycle;
  private long ticks;

  private long cyclesBetweenActivations;
  private long instructionsPerSecond;

  public Counter(Machine machine, int index, long instructionsPerSecond) {
    this.machine = machine;
    this.index = index;
    this.instructionsPerSecond = instructionsPerSecond;
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
    return uint8(value);
  }

  private void writeLsb(int partialValue) {
    value |= uint8(partialValue);
  }

  private int readMsb() {
    return (value & 0xFF00) >>> 16;
  }

  private void writeMsb(int partialValue) {
    value |= (partialValue << 8) & 0xFF00;
  }

  private void onValueWrite() {
    updateDesiredFreqency(HARDWARE_FREQUENCY / value);
  }

  private void updateDesiredFreqency(long desiredFrequency) {
    cyclesBetweenActivations = this.instructionsPerSecond / desiredFrequency;
  }

  /**
   * @param currentCycles
   *          current cycle count of the CPU
   * @return true if latched, false otherwise
   */
  public boolean processActivation(long currentCycles) {
    if (currentCycles > lastActivationCycle + cyclesBetweenActivations) {
      lastActivationCycle = currentCycles;
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
