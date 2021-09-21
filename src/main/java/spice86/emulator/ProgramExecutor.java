package spice86.emulator;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.cpu.Cpu;
import spice86.emulator.cpu.State;
import spice86.emulator.errors.InvalidOperationException;
import spice86.emulator.errors.UnrecoverableException;
import spice86.emulator.function.FunctionHandler;
import spice86.emulator.function.FunctionInformation;
import spice86.emulator.function.OverrideSupplier;
import spice86.emulator.gdb.GdbServer;
import spice86.emulator.loadablefile.ExecutableFileLoader;
import spice86.emulator.loadablefile.bios.BiosLoader;
import spice86.emulator.loadablefile.dos.com.ComLoader;
import spice86.emulator.loadablefile.dos.exe.ExeLoader;
import spice86.emulator.machine.Machine;
import spice86.emulator.memory.SegmentedAddress;
import spice86.ui.Gui;

/**
 * Loads and executes a program following the given configuration in the emulator.<br/>
 * Currently only supports DOS exe files.
 */
public class ProgramExecutor implements java.io.Closeable {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProgramExecutor.class);

  private Machine machine;
  private GdbServer gdbServer;

  public ProgramExecutor(Gui gui, Configuration configuration) {
    createMachine(gui, configuration);
  }

  public Machine getMachine() {
    return machine;
  }

  public void run() throws InvalidOperationException {
    machine.run();
  }

  private final void createMachine(Gui gui, Configuration configuration) {
    machine = new Machine(gui, configuration.getInstructionsPerSecond());
    initializeCpu();
    initializeDos(configuration);
    initializeFunctionHandlers(configuration);
    if (configuration.isInstallInterruptVector()) {
      // Doing this after function Handler init so that custom code there can have a chance to register some callbacks
      // if needed
      machine.installAllCallbacksInInterruptTable();
    }
    loadFileToRun(configuration);
    startGdbServer(configuration);
  }

  private void initializeCpu() {
    Cpu cpu = machine.getCpu();
    cpu.setErrorOnUninitializedInterruptHandler(true);
    State state = cpu.getState();
    state.getFlags().setDosboxCompatibility(true);
  }

  private void initializeDos(Configuration configuration) {
    Map<Character, String> driveMap = new HashMap<>();
    driveMap.put('C', configuration.getcDrive());
    String parentFolder = getParentFolder(configuration);
    machine.getDosInt21Handler().getDosFileManager().setDiskParameters(parentFolder, driveMap);
  }

  private void initializeFunctionHandlers(Configuration configuration) {
    Cpu cpu = machine.getCpu();
    Map<SegmentedAddress, FunctionInformation> functionInformations =
        generateFunctionInformations(configuration.getOverrideSupplier(), configuration.getProgramEntryPointSegment(),
            machine);
    boolean useCodeOverride = configuration.isUseCodeOverride();
    setupFunctionHandler(cpu.getFunctionHandler(), functionInformations, useCodeOverride);
    setupFunctionHandler(cpu.getFunctionHandlerInExternalInterrupt(), functionInformations, useCodeOverride);
  }

  private void startGdbServer(Configuration configuration) {
    Integer gdbPort = configuration.getGdbPort();
    if (gdbPort != null) {
      gdbServer = new GdbServer(machine, gdbPort);
    }
  }

  private String getParentFolder(Configuration configuration) {
    return (Paths.get(configuration.getExe()).toFile().getParent() + '/').replace('\\', '/');
  }

  private void loadFileToRun(Configuration configuration) {
    String fileName = configuration.getExe();
    ExecutableFileLoader loader = createExecutableFileLoader(fileName, configuration.getProgramEntryPointSegment());
    LOGGER.info("Loading file {} with loader {}", fileName, loader.getClass());
    try {
      loader.loadFile(fileName);
    } catch (IOException e) {
      throw new UnrecoverableException("Failed to read exe file " + fileName, e);
    }
  }

  private ExecutableFileLoader createExecutableFileLoader(String fileName, int entryPointSegment) {
    String lowerCaseFileName = fileName.toLowerCase();
    if (lowerCaseFileName.endsWith(".exe")) {
      return new ExeLoader(machine, entryPointSegment);
    } else if (lowerCaseFileName.endsWith(".com")) {
      return new ComLoader(machine, entryPointSegment);
    }
    return new BiosLoader(machine);
  }

  @Override
  public void close() {
    machine.getCpu().setRunning(false);
    if (gdbServer != null) {
      gdbServer.close();
    }
  }

  private void setupFunctionHandler(FunctionHandler functionHandler,
      Map<SegmentedAddress, FunctionInformation> functionInformations, boolean useCodeOverride) {
    functionHandler.setFunctionInformations(functionInformations);
    functionHandler.setUseCodeOverride(useCodeOverride);
  }

  private Map<SegmentedAddress, FunctionInformation>
      generateFunctionInformations(OverrideSupplier supplier, int entryPointSegment, Machine machine) {
    Map<SegmentedAddress, FunctionInformation> res = new HashMap<>();
    if (supplier != null) {
      LOGGER.info("Override supplied: {}", supplier);
      res.putAll(supplier.generateFunctionInformations(entryPointSegment, machine));
    }
    return res;
  }
}
