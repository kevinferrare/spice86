package spice86.emulator.callback;

import java.util.Comparator;

import spice86.emulator.errors.UnhandledOperationException;
import spice86.emulator.machine.Machine;
import spice86.emulator.memory.Memory;
import spice86.emulator.memory.MemoryUtils;

/**
 * Holds a Callback for each callback number and calls it depending on the callback number given.<br/>
 * Handles the installation of the interrupts in the vector with the special callback instruction in memory as well.
 */
public class CallbackHandler extends IndexBasedDispatcher<Callback> {
  private Machine machine;
  private Memory memory;
  // Segment where to install the callbacks code in memory
  private int callbackHandlerSegment;
  // offset in this segment so that new callbacks are written to a fresh location
  private int offset = 0;

  public CallbackHandler(Machine machine, int interruptHandlerSegment) {
    this.machine = machine;
    this.memory = machine.getMemory();
    this.callbackHandlerSegment = interruptHandlerSegment;
  }

  public void addCallback(Callback callback) {
    addService(callback.getIndex(), callback);
  }

  @Override
  protected UnhandledOperationException generateUnhandledOperationException(int index) {
    return new UnhandledCallbackException(machine, index);
  }

  public void installAllCallbacksInInterruptTable() {
    this.dispatchTable.values()
        .stream()
        .sorted(Comparator.comparing(Callback::getIndex))
        .forEach(this::installCallbackInInterruptTable);
  }

  private void installCallbackInInterruptTable(Callback callback) {
    offset += installInterruptWithCallback(callback.getIndex(), callbackHandlerSegment, offset);
  }

  private int installInterruptWithCallback(int vectorNumber, int segment, int offset) {
    installVectorInTable(vectorNumber, segment, offset);
    return writeInterruptCallback(vectorNumber, segment, offset);
  }

  private int writeInterruptCallback(int vectorNumber, int segment, int offset) {
    int address = MemoryUtils.toPhysicalAddress(segment, offset);
    // CALLBACK opcode (custom instruction, FE38 + 16 bits callback number)
    memory.setUint8(address, 0xFE);
    memory.setUint8(address + 1, 0x38);
    // vector to call
    memory.setUint16(address + 2, vectorNumber);
    // IRET
    memory.setUint8(address + 4, 0xCF);
    // 5 bytes used
    return 5;
  }

  private void installVectorInTable(int vectorNumber, int segment, int offset) {
    // install the vector in the vector table
    memory.setUint16(4 * vectorNumber + 2, segment);
    memory.setUint16(4 * vectorNumber, offset);
  }
}
