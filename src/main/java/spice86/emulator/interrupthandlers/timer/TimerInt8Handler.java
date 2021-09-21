package spice86.emulator.interrupthandlers.timer;

import spice86.emulator.devices.externalinterrupt.Pic;
import spice86.emulator.devices.timer.Timer;
import spice86.emulator.errors.UnhandledOperationException;
import spice86.emulator.interrupthandlers.InterruptHandler;
import spice86.emulator.machine.Machine;
import spice86.emulator.memory.MemoryMap;
import spice86.emulator.memory.MemoryUtils;

/**
 * Implementation of int8 that just updates a value in the bios data area.
 */
public class TimerInt8Handler extends InterruptHandler {
  private static final int BIOS_DATA_AREA_OFFSET_TICK_COUNTER_ADDRESS =
      MemoryUtils.toPhysicalAddress(MemoryMap.BIOS_DATA_AREA_SEGMENT, MemoryMap.BIOS_DATA_AREA_OFFSET_TICK_COUNTER);

  private Timer timer;
  private Pic pic;

  public TimerInt8Handler(Machine machine) {
    super(machine);
    this.timer = machine.getTimer();
    this.memory = machine.getMemory();
    this.pic = machine.getPic();
  }

  @Override
  public void run() throws UnhandledOperationException {
    long numberOfTicks = timer.getNumberOfTicks();
    setTickCounterValue((int)numberOfTicks);
    pic.acknwowledgeInterrupt();
  }

  public int getTickCounterValue() {
    return memory.getUint32(BIOS_DATA_AREA_OFFSET_TICK_COUNTER_ADDRESS);
  }

  public void setTickCounterValue(int value) {
    memory.setUint32(BIOS_DATA_AREA_OFFSET_TICK_COUNTER_ADDRESS, value);
  }

  @Override
  public int getIndex() {
    return 0x8;
  }
}
