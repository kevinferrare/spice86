package spice86.emulator.gdb;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.cpu.Cpu;
import spice86.emulator.function.CallType;
import spice86.emulator.function.dump.CsvFunctionInformationToStringConverter;
import spice86.emulator.function.dump.DetailedFunctionInformationToStringConverter;
import spice86.emulator.function.dump.FunctionInformationDumper;
import spice86.emulator.function.dump.FunctionInformationToStringConverter;
import spice86.emulator.function.dump.JavaStubToStringConverter;
import spice86.emulator.machine.Machine;
import spice86.emulator.machine.breakpoint.BreakPoint;
import spice86.emulator.machine.breakpoint.BreakPointType;
import spice86.emulator.machine.breakpoint.UnconditionalBreakPoint;
import spice86.utils.CheckedConsumer;
import spice86.utils.ConvertUtils;

/**
 * Handles custom GDB commands triggered in command line via the monitor prefix.<br/>
 * Custom commands list can be seen with the monitor help command.
 */
public class GdbCustomCommandsHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(GdbCustomCommandsHandler.class);

  private GdbIo gdbIo;
  private Machine machine;
  private Consumer<BreakPoint> onBreakpointReached;

  public GdbCustomCommandsHandler(GdbIo gdbIo, Machine machine, Consumer<BreakPoint> onBreakpointReached) {
    this.gdbIo = gdbIo;
    this.machine = machine;
    this.onBreakpointReached = onBreakpointReached;
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
      case "dumpfunctionscsv" -> dumpFunctionWithFormat(args, "Functions.csv",
          new CsvFunctionInformationToStringConverter());
      case "dumpfunctions" -> dumpFunctionWithFormat(args, "FunctionsDetails.txt",
          new DetailedFunctionInformationToStringConverter());
      case "dumpjavastubs" -> dumpFunctionWithFormat(args, "JavaStubs.java", new JavaStubToStringConverter());
      case "breakcycles" -> breakCycles(args);
      default -> invalidCommand(originalCommand);
    };
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
        """
            Supported custom commands:
             - help: display this
             - dumpMemory <file path to dump>: dump the memory as a binary file
             - dumpFunctionsCsv <file path to dump>: dump information about the function calls executed in csv format
             - dumpFunctions <file path to dump>: dump information about the function calls executed with details in human readable format
             - dumpJavaStubs <file path to dump>: dump java stubs for functions to be used as override
             - breakCycles <number of cycles to wait before break>: breaks after the given number of cycles is reached
             - breakStop: setups a breakpoint when machine shuts down
             - callStack: dumps the callstack to see in which function you are in the VM.
             - peekRet <optional type>: displays the return address of the current function as stored in the stack in RAM. If a parameter is provided, dump the return on the stack as if the return was one of the provided type. Valid values are: """
        + getValidRetValues() + """
             - state: displays the state of the machine
            """);
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
    return "spice86dump" + defaultSuffix;
  }
}
