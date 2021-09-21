package spice86.emulator.machine.breakpoint;

public enum BreakPointType {
  /**
   * CPU breakpoint triggered when address of instruction to be executed matches the address specified in the
   * breakpoint.
   */
  EXECUTION,
  /**
   * CPU breakpoint triggered when the number of cycles executed by the CPU reach the number specified in the breakpoint
   * address.
   */
  CYCLES,
  /**
   * Memory breakpoint triggered when memory is read at the address specified in the breakpoint.
   */
  READ,
  /**
   * Memory breakpoint triggered when memory is written at the address specified in the breakpoint.
   */
  WRITE,
  /**
   * Memory breakpoint triggered when memory is read or written at the address specified in the breakpoint.
   */
  ACCESS,
  /**
   * Breakpoint is triggered when the machine stops.
   */
  MACHINE_STOP
}
