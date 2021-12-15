package spice86.emulator.gdb;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spice86.emulator.cpu.Cpu;
import spice86.emulator.devices.video.VgaCard;
import spice86.emulator.function.CallType;
import spice86.emulator.function.dump.CsvFunctionInformationToStringConverter;
import spice86.emulator.function.dump.DetailedFunctionInformationToStringConverter;
import spice86.emulator.function.dump.FunctionInformationDumper;
import spice86.emulator.function.dump.FunctionInformationToStringConverter;
import spice86.emulator.function.dump.JavaStubToStringConverter;
import spice86.emulator.function.dump.KotlinStubToStringConverter;
import spice86.emulator.machine.Machine;
import spice86.emulator.machine.breakpoint.BreakPoint;
import spice86.emulator.machine.breakpoint.BreakPointType;
import spice86.emulator.machine.breakpoint.UnconditionalBreakPoint;
import spice86.emulator.memory.Memory;
import spice86.ui.Gui;
import spice86.ui.VideoBuffer;
import spice86.utils.CheckedConsumer;
import spice86.utils.ConvertUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Handles custom GDB commands triggered in command line via the monitor prefix.<br/>
 * Custom commands list can be seen with the monitor help command.
 */
public class GdbCustomCommandsHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(GdbCustomCommandsHandler.class);

  private GdbIo gdbIo;
  private Machine machine;
  private Consumer<BreakPoint> onBreakpointReached;
  private String defaultDumpDirectory;

  public GdbCustomCommandsHandler(GdbIo gdbIo, Machine machine, Consumer<BreakPoint> onBreakpointReached,
      String defaultDumpDirectory) {
    this.gdbIo = gdbIo;
    this.machine = machine;
    this.onBreakpointReached = onBreakpointReached;
    this.defaultDumpDirectory = defaultDumpDirectory;
  }

  public String handleCustomCommands(String command) {
    String[] commandSplit = command.split(",");
    if (commandSplit.length != 2) {
      return gdbIo.generateResponse("");
    }
    byte[] customHex = ConvertUtils.hexToByteArray(commandSplit[1]);
    String custom = new String(customHex, StandardCharsets.UTF_8);
    String[] customSplit = custom.split(" ");
    return executeCustomCommand(customSplit);
  }

  private String executeCustomCommand(String... args) {
    String originalCommand = args[0];
    String command = originalCommand.toLowerCase();
    return switch (command) {
      case "help" -> help("");
      case "state" -> state();
      case "breakstop" -> breakStop();
      case "callstack" -> callStack();
      case "peekret" -> peekRet(args);
      case "dumpmemory" -> dumpMemory(args);
      case "dumpfunctionscsv" -> dumpFunctionsCsv(args);
      case "dumpfunctions" -> dumpFunctions(args);
      case "dumpjavastubs" -> dumpJavaStubs(args);
      case "dumpkotlinstubs" -> dumpKotlinStubs(args);
      case "dumpall" -> dumpAll();
      case "breakcycles" -> breakCycles(args);
      case "vbuffer" -> vbuffer(args);
      default -> invalidCommand(originalCommand);
    };
  }

  private String dumpFunctionsCsv(String[] args) {
    return dumpFunctionWithFormat(args, "Functions.csv", new CsvFunctionInformationToStringConverter());
  }

  private String dumpFunctions(String[] args) {
    return dumpFunctionWithFormat(args, "FunctionsDetails.txt", new DetailedFunctionInformationToStringConverter());
  }

  private String dumpJavaStubs(String[] args) {
    return dumpFunctionWithFormat(args, "JavaStubs.java", new JavaStubToStringConverter());
  }

  private String dumpKotlinStubs(String[] args) {
    return dumpFunctionWithFormat(args, "KotlinStubs.kt", new KotlinStubToStringConverter());
  }

  private String dumpAll() {
    String[] args = new String[0];
    dumpMemory(args);
    dumpFunctionsCsv(args);
    dumpFunctions(args);
    dumpJavaStubs(args);
    dumpKotlinStubs(args);
    return gdbIo.generateMessageToDisplayResponse("Dumped everything in " + defaultDumpDirectory);
  }

  private String peekRet(String[] args) {
    if (args.length == 1) {
      return gdbIo.generateMessageToDisplayResponse(machine.peekReturn());
    } else {
      String returnType = args[1];
      CallType callType = EnumUtils.getEnumIgnoreCase(CallType.class, returnType);
      if (callType == null) {
        return gdbIo.generateMessageToDisplayResponse(
            "Could not understand " + returnType + " as a return type. Valid values are: " + getValidRetValues());
      }
      return gdbIo.generateMessageToDisplayResponse(machine.peekReturn(callType));
    }
  }

  private String invalidCommand(String command) {
    return help("Invalid command " + command + "\n");
  }

  private String getValidRetValues() {
    return StringUtils.join(CallType.values(), ", ");
  }

  private String help(String additionalMessage) {
    return gdbIo.generateMessageToDisplayResponse(additionalMessage +
        MessageFormat.format("""
            Supported custom commands:
             - help: display this
             - dumpall: dumps everything possible in the default directory which is {0}
             - dumpMemory <file path to dump>: dump the memory as a binary file
             - dumpFunctionsCsv <file path to dump>: dump information about the function calls executed in csv format
             - dumpFunctions <file path to dump>: dump information about the function calls executed with details in human readable format
             - dumpJavaStubs <file path to dump>: dump java stubs for functions and globals to be used as override
             - dumpKotlinStubs <file path to dump>: dump kotlin stubs for functions and globals to be used as override
             - breakCycles <number of cycles to wait before break>: breaks after the given number of cycles is reached
             - breakStop: setups a breakpoint when machine shuts down
             - callStack: dumps the callstack to see in which function you are in the VM.
             - peekRet <optional type>: displays the return address of the current function as stored in the stack in RAM. If a parameter is provided, dump the return on the stack as if the return was one of the provided type. Valid values are: {1}
             - state: displays the state of the machine
             - vbuffer: family of commands to control video bufers:
               - vbuffer refresh: refreshes the screen
               - vbuffer add <address> <resolution> <scale?>: Example vbuffer add 0x1234 320x200 1.5 -> Add an additional buffer displaying what is at address 0x1234, with resolution 320x200 and scale 1.5
               - vbuffer remove <address>: Deletes the buffer at address
               - vbuffer list: Lists the buffers currently displayed
            """, this.defaultDumpDirectory, getValidRetValues()));
  }

  private String state() {
    String state = machine.getCpu().getState().toString();
    return gdbIo.generateMessageToDisplayResponse(state);
  }

  private String breakStop() {
    BreakPoint breakPoint =
        new UnconditionalBreakPoint(BreakPointType.MACHINE_STOP, onBreakpointReached, false);
    machine.getMachineBreakpoints().toggleBreakPoint(breakPoint, true);
    LOGGER.debug("Breakpoint added for end of execution!\n{}", breakPoint);
    return gdbIo.generateMessageToDisplayResponse("Breakpoint added for end of execution.");
  }

  private String callStack() {
    return gdbIo.generateMessageToDisplayResponse(machine.dumpCallStack());
  }

  private String breakCycles(String[] args) {
    if (args.length < 2) {
      return invalidCommand("breakCycles can only work with one argument.");
    }
    String cyclesToWaitString = args[1];
    if (!NumberUtils.isParsable(cyclesToWaitString)) {
      return invalidCommand("breakCycles argument needs to be a number. You gave " + cyclesToWaitString);
    }
    long cyclesToWait = Long.parseLong(cyclesToWaitString);
    long currentCycles = machine.getCpu().getState().getCycles();
    long cyclesBreak = currentCycles + cyclesToWait;
    BreakPoint breakPoint =
        new BreakPoint(BreakPointType.CYCLES, cyclesBreak, onBreakpointReached, true);
    machine.getMachineBreakpoints().toggleBreakPoint(breakPoint, true);
    LOGGER.debug("Breakpoint added for cycles!\n{}", breakPoint);
    return gdbIo.generateMessageToDisplayResponse(
        "Breakpoint added for cycles. Current cycles is " + currentCycles + ". Will wait for " + cyclesToWait
            + ". Will stop at " + cyclesBreak);
  }

  private String dumpMemory(String[] args) {
    String fileName = getFirstArgumentOrDefaultFile(args, "MemoryDump.bin");
    return doFileAction(fileName, f -> machine.getMemory().dumpToFile(f), "Error while dumping memory");
  }

  private String dumpFunctionWithFormat(String[] args, String defaultSuffix,
      FunctionInformationToStringConverter converter) {
    String fileName = getFirstArgumentOrDefaultFile(args, defaultSuffix);
    return doFileAction(fileName, f -> {
      Cpu cpu = machine.getCpu();
      new FunctionInformationDumper().dumpFunctionHandlers(f, converter, cpu.getStaticAddressesRecorder(),
          cpu.getFunctionHandler(),
          cpu.getFunctionHandlerInExternalInterrupt());
    }, "Error while dumping functions");
  }

  private String doFileAction(String fileName, CheckedConsumer<String, IOException> fileNameConsumer,
      String errorMessageInCaseIOException) {
    try {
      fileNameConsumer.accept(fileName);
    } catch (IOException e) {
      LOGGER.error(errorMessageInCaseIOException, e);
      String errorWithException = errorMessageInCaseIOException + ": " + e.getMessage();
      return gdbIo.generateMessageToDisplayResponse(errorWithException);
    }
    return resultIsInFile(fileName);
  }

  private String resultIsInFile(String fileName) {
    return gdbIo.generateMessageToDisplayResponse("Result is in file " + fileName);
  }

  private String getFirstArgumentOrDefaultFile(String[] args, String defaultSuffix) {
    if (args.length >= 2) {
      return args[1];
    }
    return defaultDumpDirectory + "/spice86dump" + defaultSuffix;
  }

  private String vbuffer(String[] args) {
    try {
      String action = extractAction(args);
      Gui gui = machine.getGui();
      VgaCard vgaCard = machine.getVgaCard();
      // Actions for 1 parameter
      if ("refresh".equals(action)) {
        Memory memory = machine.getMemory();
        gui.draw(memory.getRam(), vgaCard.getVgaDac().getRgbs());
        return gdbIo.generateResponse("");
      } else if ("list".equals(action)) {
        String list = gui.getVideoBuffers().values().stream().map(Object::toString).collect(Collectors.joining("\n"));
        return gdbIo.generateMessageToDisplayResponse(list);
      }

      int address = extractAddress(args, action);
      if ("remove".equals(action)) {
        gui.removeBuffer(address);
        return gdbIo.generateMessageToDisplayResponse("Removed buffer at address " + address);
      }
      int[] resolution = extractResolution(args, action);
      double scale = extractScale(args);
      if ("add".equals(action)) {
        VideoBuffer existing = gui.getVideoBuffers().get(address);
        if (existing != null) {
          return gdbIo.generateMessageToDisplayResponse("Buffer already exists: " + existing);
        }
        gui.addBuffer(address, scale, resolution[0], resolution[1], null);
        return gdbIo.generateMessageToDisplayResponse("Added buffer to view address " + address);
      } else {
        return gdbIo.generateMessageToDisplayResponse("Could not understand action " + action);
      }
    } catch (IllegalArgumentException e) {
      return gdbIo.generateMessageToDisplayResponse(e.getMessage());
    }
  }

  private String extractAction(String[] args) throws IllegalArgumentException {
    if (args.length >= 2) {
      return args[1];
    }
    throw new IllegalArgumentException("You need to specify an action. Valid actions are [refresh, add, remove]");
  }

  private int extractAddress(String[] args, String action) throws IllegalArgumentException {
    if (args.length < 3) {
      throw new IllegalArgumentException(
          "You need to specify an address for action " + action + ". Format is 0x12AB (hex) or 1234 (decimal)");
    }
    String addressString = args[2];
    try {
      return parseAddress(addressString);
    } catch (NumberFormatException nfe) {
      throw new IllegalArgumentException("Could not parse address " + addressString, nfe);
    }
  }

  private int parseAddress(String address) throws NumberFormatException {
    if (address.contains("0x")) {
      return (int)ConvertUtils.parseHex32(address);
    }
    return Integer.parseInt(address);
  }

  private int[] extractResolution(String[] args, String action) throws IllegalArgumentException {
    if (args.length < 4) {
      throw new IllegalArgumentException(
          "You need to specify a resolution for action " + action + ". Format is 320x200 for resolution");
    }
    String resolutionString = args[3];
    return parseResolution(resolutionString);
  }

  private int[] parseResolution(String resolution) throws IllegalArgumentException {
    String[] split = resolution.split("x");
    if (split.length != 2) {
      throw new IllegalArgumentException("Could not parse resolution " + resolution + ". Format is like 320x200");
    }
    try {
      return new int[] { Integer.parseInt(split[0]), Integer.parseInt(split[1]) };
    } catch (NumberFormatException nfe) {
      throw new IllegalArgumentException("Could not parse numbers in resolution " + resolution, nfe);
    }
  }

  private double extractScale(String[] args) throws IllegalArgumentException {
    if (args.length != 5) {
      // Not specified in input
      return 1.0d;
    }
    String scaleString = args[4];
    if (!NumberUtils.isParsable(scaleString)) {
      throw new IllegalArgumentException("Could not parse scale " + scaleString);
    }
    double scale = Double.parseDouble(scaleString);
    if (scale < 0.1) {
      throw new IllegalArgumentException("Scale cannot be less than 0.1");
    }
    return scale;
  }
}
