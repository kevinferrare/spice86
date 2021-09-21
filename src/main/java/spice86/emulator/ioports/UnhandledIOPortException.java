package spice86.emulator.ioports;

import spice86.emulator.errors.UnhandledOperationException;
import spice86.emulator.machine.Machine;
import spice86.utils.ConvertUtils;

/**
 * Thrown when an unhandled IO Port is accessed.
 */
public class UnhandledIOPortException extends UnhandledOperationException {
  public UnhandledIOPortException(Machine machine, int ioPort) {
    super(machine, "Unhandled port " + ConvertUtils.toHex(ioPort)
        + ". This usually means that the hardware behind the port is not emulated or that the port is not routed correctly.");
  }
}
