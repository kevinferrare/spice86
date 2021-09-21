package spice86.emulator.cpu;

import spice86.emulator.errors.InvalidOperationException;
import spice86.emulator.machine.Machine;
import spice86.utils.ConvertUtils;

/**
 * Thrown when the CPU encounters an unimplemented or invalid opcode.
 */
public class InvalidOpcodeException extends InvalidOperationException {
  public InvalidOpcodeException(Machine machine, int opcode, boolean prefixNotAllowed) {
    super(machine, generateMessage(opcode, prefixNotAllowed));
  }

  private static String generateMessage(int opcode, boolean prefixNotAllowed) {
    return "opcode=" + ConvertUtils.toHex(opcode) + (prefixNotAllowed ? " prefix is not allowed here" : "");
  }
}
