package spice86.emulator.function;

/**
 * Represents x86 call types.
 */
public enum CallType {
  // For this call, only IP is on the stack
  NEAR,
  // For this call, CS and IP are on the stack
  FAR,
  // For this call, CS, IP and the flags are on the stack
  INTERRUPT,
  // Means called by the VM itself and not by emulated code.
  MACHINE
}
