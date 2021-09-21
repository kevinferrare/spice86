package spice86.emulator.gdb;

import static spice86.utils.ConvertUtils.parseHex32;
import static spice86.utils.ConvertUtils.uint32i;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.machine.Machine;
import spice86.emulator.machine.breakpoint.BreakPoint;
import spice86.emulator.machine.breakpoint.BreakPointType;
import spice86.emulator.machine.breakpoint.UnconditionalBreakPoint;

public class GdbCommandBreakpointHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(GdbCommandBreakpointHandler.class);
  private GdbIo gdbIo;
  private Machine machine;
  private volatile boolean resumeEmulatorOnCommandEnd;

  public GdbCommandBreakpointHandler(GdbIo gdbIo, Machine machine) {
    this.gdbIo = gdbIo;
    this.machine = machine;
  }

  public boolean isResumeEmulatorOnCommandEnd() {
    return resumeEmulatorOnCommandEnd;
  }

  public void setResumeEmulatorOnCommandEnd(boolean resumeEmulatorOnCommandEnd) {
    this.resumeEmulatorOnCommandEnd = resumeEmulatorOnCommandEnd;
  }

  public String addBreakpoint(String commandContent) {
    BreakPoint breakPoint = parseBreakPoint(commandContent);
    machine.getMachineBreakpoints().toggleBreakPoint(breakPoint, true);
    LOGGER.debug("Breakpoint added!\n{}", breakPoint);
    return gdbIo.generateResponse("OK");
  }

  public String removeBreakpoint(String commandContent) {
    BreakPoint breakPoint = parseBreakPoint(commandContent);
    machine.getMachineBreakpoints().toggleBreakPoint(breakPoint, false);
    LOGGER.debug("Breakpoint removed!\n{}", breakPoint);
    return gdbIo.generateResponse("OK");
  }

  public String step() {
    resumeEmulatorOnCommandEnd = true;
    // will pause the CPU at the next instruction unconditionally
    BreakPoint stepBreakPoint = new UnconditionalBreakPoint(BreakPointType.EXECUTION, this::onBreakPointReached, true);
    machine.getMachineBreakpoints().toggleBreakPoint(stepBreakPoint, true);
    LOGGER.debug("Breakpoint added for step!\n{}", stepBreakPoint);
    // Do not send anything to GDB, CPU thread will send something when breakpoint is reached
    return null;
  }

  public BreakPoint parseBreakPoint(String command) {
    try {
      String[] commandSplit = command.split(",");
      int type = Integer.parseInt(commandSplit[0]);
      int address = uint32i(parseHex32(commandSplit[1]));
      // 3rd parameter kind is unused in our case
      BreakPointType breakPointType = switch (type) {
        case 0, 1 -> BreakPointType.EXECUTION;
        case 2 -> BreakPointType.WRITE;
        case 3 -> BreakPointType.READ;
        case 4 -> BreakPointType.ACCESS;
        default -> null;
      };
      if (breakPointType == null) {
        LOGGER.error("Cannot parse breakpoint type {} for command {}", type, command);
      }
      return new BreakPoint(breakPointType, address, this::onBreakPointReached, false);
    } catch (NumberFormatException nfe) {
      LOGGER.error("Cannot parse breakpoint {}", command);
      return null;
    }
  }

  public void onBreakPointReached(BreakPoint breakPoint) {
    LOGGER.debug("Breakpoint reached!\n{}", breakPoint);
    machine.getMachineBreakpoints().getPauseHandler().requestPause();
    resumeEmulatorOnCommandEnd = false;
    try {
      gdbIo.sendResponse(gdbIo.generateResponse("S05"));
    } catch (IOException e) {
      LOGGER.error("IOException while sending breakpoint info", e);
    }
  }

  public String continueCommand() {
    resumeEmulatorOnCommandEnd = true;
    machine.getMachineBreakpoints().getPauseHandler().requestResume();
    // Do not send anything to GDB, CPU thread will send something when breakpoint is reached
    return gdbIo.generateResponse("OK");
  }

}
