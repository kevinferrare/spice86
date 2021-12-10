package spice86.emulator.gdb;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.machine.Machine;
import spice86.emulator.machine.PauseHandler;

/**
 * Parser and executor for GDB commands sent by the client.
 */
public class GdbCommandHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(GdbCommandHandler.class);

  private GdbIo gdbIo;
  private Machine machine;
  private boolean connected = true;
  private GdbCommandRegisterHandler gdbCommandRegisterHandler;
  private GdbCommandMemoryHandler gdbCommandMemoryHandler;
  private GdbCustomCommandsHandler gdbCustomCommandsHandler;
  private GdbCommandBreakpointHandler gdbCommandBreakpointHandler;

  public GdbCommandHandler(GdbIo gdbIo, Machine machine, String defaultDumpDirectory) {
    this.gdbIo = gdbIo;
    this.machine = machine;
    this.gdbCommandRegisterHandler = new GdbCommandRegisterHandler(gdbIo, machine);
    this.gdbCommandMemoryHandler = new GdbCommandMemoryHandler(gdbIo, machine);
    this.gdbCommandBreakpointHandler = new GdbCommandBreakpointHandler(gdbIo, machine);
    this.gdbCustomCommandsHandler =
        new GdbCustomCommandsHandler(gdbIo, machine, gdbCommandBreakpointHandler::onBreakPointReached, defaultDumpDirectory);
  }

  public boolean isConnected() {
    return connected;
  }

  public void runCommand(String command) throws IOException {
    LOGGER.info("Received command {}", command);
    char first = command.charAt(0);
    String commandContent = StringUtils.substring(command, 1);
    PauseHandler pauseHandler = machine.getMachineBreakpoints().getPauseHandler();
    pauseHandler.requestPauseAndWait();
    try {
      String response = switch (first) {
        case 0x03 -> gdbCommandBreakpointHandler.step();
        case 'k' -> kill();
        case 'D' -> detach();
        case 'c' -> gdbCommandBreakpointHandler.continueCommand();
        case 'H' -> setThreadContext();
        case 'q' -> queryVariable(commandContent);
        case '?' -> reasonHalted();
        case 'g' -> gdbCommandRegisterHandler.readAllRegisters();
        case 'G' -> gdbCommandRegisterHandler.writeAllRegisters(commandContent);
        case 'p' -> gdbCommandRegisterHandler.readRegister(commandContent);
        case 'P' -> gdbCommandRegisterHandler.writeRegister(commandContent);
        case 'm' -> gdbCommandMemoryHandler.readMemory(commandContent);
        case 'M' -> gdbCommandMemoryHandler.writeMemory(commandContent);
        case 'T' -> handleThreadALive();
        case 'v' -> processVPacket(commandContent);
        case 's' -> gdbCommandBreakpointHandler.step();
        case 'z' -> gdbCommandBreakpointHandler.removeBreakpoint(commandContent);
        case 'Z' -> gdbCommandBreakpointHandler.addBreakpoint(commandContent);
        default -> gdbIo.generateUnsupportedResponse();
      };
      if (response != null) {
        gdbIo.sendResponse(response);
      }
    } finally {
      if (gdbCommandBreakpointHandler.isResumeEmulatorOnCommandEnd()) {
        pauseHandler.requestResumeAndWait();
      }
    }
  }

  private String handleThreadALive() {
    return gdbIo.generateResponse("OK");
  }

  public void pauseEmulator() {
    gdbCommandBreakpointHandler.setResumeEmulatorOnCommandEnd(false);
    machine.getMachineBreakpoints().getPauseHandler().requestPause();
  }

  private String setThreadContext() {
    // always OK, we only have one thread anyway
    return gdbIo.generateResponse("OK");
  }

  private String reasonHalted() {
    // dummy response
    return gdbIo.generateResponse("S05");
  }

  private String queryVariable(String command) {
    if (command.startsWith("Supported:")) {
      String[] supportedRequestItems = command.replace("Supported:", "").split(";");
      Map<String, Object> supportedRequest = Arrays.stream(supportedRequestItems)
          .map(this::parseSupportedQuery)
          .collect(Collectors.toMap(data -> (String)data[0], data -> data[1]));
      if (!"i386".equals(supportedRequest.get("xmlRegisters"))) {
        return gdbIo.generateUnsupportedResponse();
      }
      return gdbIo.generateResponse("");
    }
    if (command.startsWith("L")) {
      // qL startflag threadcount nextthread => qM count done argthread thread…
      String nextthread = StringUtils.substring(command, 4);
      return gdbIo.generateResponse("qM011" + nextthread + "00000001");
    }
    if (command.startsWith("P")) {
      // deprecated thread info
      return gdbIo.generateResponse("");
    }
    if (command.startsWith("ThreadExtraInfo")) {
      return gdbIo.generateMessageToDisplayResponse("spice86");
    }
    if (command.startsWith("Rcmd")) {
      return gdbCustomCommandsHandler.handleCustomCommands(command);
    }
    if (command.startsWith("Search")) {
      return gdbCommandMemoryHandler.searchMemory(command);
    }
    return switch (command) {
      // The remote server attached to an existing process.
      case "Attached" -> gdbIo.generateResponse("1");
      // Return the current thread ID.
      case "C" -> gdbIo.generateResponse("QC1");
      // Ask the stub if there is a trace experiment running right now. -> No trace has been run yet.
      case "TStatus" -> gdbIo.generateResponse("");
      case "fThreadInfo" -> gdbIo.generateResponse("m1");
      case "sThreadInfo" -> gdbIo.generateResponse("l");
      default -> gdbIo.generateUnsupportedResponse();
    };
  }

  private Object[] parseSupportedQuery(String item) {
    Object[] res = new Object[2];
    if (item.endsWith("+")) {
      res[0] = item.substring(0, item.length() - 1);
      res[1] = true;
    } else if (item.endsWith("-")) {
      res[0] = item.substring(0, item.length() - 1);
      res[1] = false;
    } else {
      String[] split = item.split("=");
      res[0] = split[0];
      if (split.length == 2) {
        res[1] = split[1];
      }
    }
    return res;
  }

  private String processVPacket(String commandContent) {
    return switch (commandContent) {
      case "MustReplyEmpty" -> gdbIo.generateResponse("");
      case "Cont?" -> gdbIo.generateResponse("");
      default -> gdbIo.generateUnsupportedResponse();
    };
  }

  private String kill() {
    machine.getCpu().setRunning(false);
    return detach();
  }

  private String detach() {
    connected = false;
    gdbCommandBreakpointHandler.setResumeEmulatorOnCommandEnd(true);
    return gdbIo.generateResponse("");
  }

}
