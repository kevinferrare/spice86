package spice86.emulator.cpu;

import static spice86.utils.ConvertUtils.int16;
import static spice86.utils.ConvertUtils.int8;
import static spice86.utils.ConvertUtils.uint16;

import spice86.emulator.function.ValueOperation;
import spice86.emulator.function.OperandSize;
import spice86.emulator.function.StaticAddressesRecorder;
import spice86.emulator.machine.Machine;
import spice86.emulator.memory.Memory;
import spice86.emulator.memory.MemoryUtils;

/**
 * Helper for CPU addressing mode byte interpretation aka Mod R/M.<br/>
 * This byte is used in a lot of instruction and determines on how to read / write data.<br/>
 * This class is stateful:
 * <ul>
 * <li>CPU state is used to read bytes (IP).</li>
 * <li>Read / write operations performed are dependant on the call to read()</li>
 * </ul>
 * <br/>
 * Some docs that helped the implementation:
 * <ul>
 * <li>https://wiki.osdev.org/X86-64_Instruction_Encoding</li>
 * <li>http://aturing.umcs.maine.edu/~meadow/courses/cos335/8086-instformat.pdf</li>
 * <li>https://software.intel.com/content/www/us/en/develop/download/intel-64-and-ia-32-architectures-sdm-combined-volumes-1-2a-2b-2c-2d-3a-3b-3c-3d-and-4.html</li>
 * </ul>
 */
public class ModRM {
  private final Machine machine;
  private final Cpu cpu;
  private final Memory memory;
  private final State state;
  private final StaticAddressesRecorder staticAddressesRecorder;

  private int registerIndex;
  private int registerMemoryIndex;
  private Integer memoryAddress;
  private Integer memoryOffset;

  /**
   * @param machine
   */
  public ModRM(Machine machine, Cpu cpu) {
    this.machine = machine;
    this.cpu = cpu;
    this.memory = machine.getMemory();
    this.state = cpu.getState();
    this.staticAddressesRecorder = cpu.getStaticAddressesRecorder();
  }

  public void read() throws InvalidModeException {
    int modRM = this.cpu.nextUint8();
    /**
     * bit 7 & bit 6 = mode bit 5 through bit 3 = registerIndex bit 2 through bit 0 = registerMemoryIndex
     */
    int mode = (modRM >>> 6) & 0b11;
    registerIndex = (modRM >>> 3) & 0b111;
    registerMemoryIndex = modRM & 0b111;
    if (mode == 3) {
      // value at reg[memoryRegisterIndex] to be used instead of memoryAddress
      memoryOffset = null;
      memoryAddress = null;
      return;
    }
    int disp = 0;
    if (mode == 1) {
      disp = int8(this.cpu.nextUint8());
    } else if (mode == 2) {
      disp = int16(this.cpu.nextUint16());
    }
    boolean bpForRm6 = mode != 0;
    memoryOffset = uint16(computeOffset(bpForRm6) + disp);
    memoryAddress = getAddress(computeDefaultSegment(bpForRm6), memoryOffset, registerMemoryIndex == 6);
  }

  private int computeDefaultSegment(boolean bpForRm6) throws InvalidModeException {
    // The default segment register is SS for the effective addresses containing a
    // BP index, DS for other effective addresses
    return switch (registerMemoryIndex) {
      case 0 -> SegmentRegisters.DS_INDEX;
      case 1 -> SegmentRegisters.DS_INDEX;
      case 2 -> SegmentRegisters.SS_INDEX;
      case 3 -> SegmentRegisters.SS_INDEX;
      case 4 -> SegmentRegisters.DS_INDEX;
      case 5 -> SegmentRegisters.DS_INDEX;
      case 6 -> bpForRm6 ? SegmentRegisters.SS_INDEX : SegmentRegisters.DS_INDEX;
      case 7 -> SegmentRegisters.DS_INDEX;
      default -> throw new InvalidModeException(machine, registerMemoryIndex);
    };
  }

  private int computeOffset(boolean bpForRm6) throws InvalidModeException {
    return switch (registerMemoryIndex) {
      case 0 -> this.state.getBX() + this.state.getSI();
      case 1 -> this.state.getBX() + this.state.getDI();
      case 2 -> this.state.getBP() + this.state.getSI();
      case 3 -> this.state.getBP() + this.state.getDI();
      case 4 -> this.state.getSI();
      case 5 -> this.state.getDI();
      case 6 -> bpForRm6 ? this.state.getBP() : this.cpu.nextUint16();
      case 7 -> this.state.getBX();
      default -> throw new InvalidModeException(machine, registerMemoryIndex);
    };
  }

  public int getAddress(int defaultSegmentRegisterIndex, int offset, boolean recordAddress) {
    Integer segmentIndex = state.getSegmentOverrideIndex();
    if (segmentIndex == null) {
      segmentIndex = defaultSegmentRegisterIndex;
    }
    if (recordAddress) {
      staticAddressesRecorder.addOffset(segmentIndex, offset);
    }
    int segment = this.state.getSegmentRegisters().getRegister(segmentIndex);
    return MemoryUtils.toPhysicalAddress(segment, offset);

  }

  public int getAddress(int defaultSegmentRegisterIndex, int offset) {
    return getAddress(defaultSegmentRegisterIndex, offset, false);
  }

  public int getRm8() {
    if (memoryAddress == null) {
      return this.state.getRegisters().getRegisterFromHighLowIndex8(registerMemoryIndex);
    }
    staticAddressesRecorder.setCurrentAddressOperation(ValueOperation.READ, OperandSize.BYTE8);
    return this.memory.getUint8(memoryAddress);
  }

  public void setRm8(int value) {
    if (memoryAddress == null) {
      this.state.getRegisters().setRegisterFromHighLowIndex8(registerMemoryIndex, value);
    } else {
      staticAddressesRecorder.setCurrentAddressOperation(ValueOperation.WRITE, OperandSize.BYTE8);
      this.memory.setUint8(memoryAddress, value);
    }
  }

  public int getRm16() {
    if (memoryAddress == null) {
      return this.state.getRegisters().getRegister(registerMemoryIndex);
    }
    staticAddressesRecorder.setCurrentAddressOperation(ValueOperation.READ, OperandSize.WORD16);
    return this.memory.getUint16(memoryAddress);
  }

  public void setRm16(int value) {
    if (memoryAddress == null) {
      this.state.getRegisters().setRegister(registerMemoryIndex, value);
    } else {
      staticAddressesRecorder.setCurrentAddressOperation(ValueOperation.WRITE, OperandSize.WORD16);
      this.memory.setUint16(memoryAddress, value);
    }
  }

  public int getR8() {
    return this.state.getRegisters().getRegisterFromHighLowIndex8(registerIndex);
  }

  public void setR8(int value) {
    this.state.getRegisters().setRegisterFromHighLowIndex8(registerIndex, value);
  }

  public int getR16() {
    return this.state.getRegisters().getRegister(registerIndex);
  }

  public void setR16(int value) {
    this.state.getRegisters().setRegister(registerIndex, value);
  }

  public void setSegmentRegister(int value) {
    this.state.getSegmentRegisters().setRegister(registerIndex, value);
  }

  public int getSegmentRegister() {
    return this.state.getSegmentRegisters().getRegister(registerIndex);
  }

  public int getRegisterIndex() {
    return registerIndex;
  }

  public Integer getMemoryOffset() {
    return memoryOffset;
  }

  public Integer getMemoryAddress() {
    return memoryAddress;
  }
}