package spice86.emulator;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spice86.emulator.cpu.Cpu;
import spice86.emulator.cpu.State;
import spice86.emulator.devices.timer.CounterConfigurator;
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
import spice86.utils.ConvertUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
    CounterConfigurator counterConfigurator = new CounterConfigurator(configuration);
    boolean debugMode = configuration.getGdbPort() != null;
    machine = new Machine(gui, counterConfigurator, configuration.isFailOnUnhandledPort(), debugMode);
    initializeCpu();
    initializeDos(configuration);
    if (configuration.isInstallInterruptVector()) {
      // Doing this after function Handler init so that custom code there can have a chance to register some callbacks
      // if needed
      machine.installAllCallbacksInInterruptTable();
    }
    initializeFunctionHandlers(configuration);
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
    String parentFolder = getExeParentFolder(configuration);
    Map<Character, String> driveMap = new HashMap<>();
    String cDrive = configuration.getcDrive();
    if(StringUtils.isEmpty(cDrive)) {
      cDrive = parentFolder;
    }
    driveMap.put('C', cDrive);
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
      gdbServer = new GdbServer(machine, gdbPort, configuration.getDefaultDumpDirectory());
    }
  }

  private String getExeParentFolder(Configuration configuration) {
    File exe = Paths.get(configuration.getExe()).toFile();
    String parent = exe.getParent();
    if (parent == null) {
      // Must be in the current directory
      parent = System.getProperty("user.dir");
    }
    parent = Paths.get(parent).toAbsolutePath().normalize().toString();
    parent = parent.replace('\\', '/') + '/';
    return parent;
  }

  private void loadFileToRun(Configuration configuration) {
    String fileName = configuration.getExe();
    ExecutableFileLoader loader = createExecutableFileLoader(fileName, configuration.getProgramEntryPointSegment());
    LOGGER.info("Loading file {} with loader {}", fileName, loader.getClass());
    try {
      byte[] fileContent = loader.loadFile(fileName);
      checkSha256Checksum(fileContent, configuration.getExpectedChecksum());
    } catch (IOException e) {
      throw new UnrecoverableException("Failed to read file " + fileName, e);
    }
  }

  private void checkSha256Checksum(byte[] file, byte[] expectedHash) {
    if (expectedHash.length == 0) {
      // No hash check
      return;
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] actualHash = digest.digest(file);
      if (!Arrays.equals(expectedHash, actualHash)) {
        String error = "File does not match the expected SHA256 checksum, cannot execute it.\n"
            + "Expected checksum is " + ConvertUtils.byteArrayToHexString(expectedHash) + ".\n"
            + "Got " + ConvertUtils.byteArrayToHexString(actualHash) + "\n";
        throw new UnrecoverableException(error);
      }
    } catch (NoSuchAlgorithmException e) {
      throw new UnrecoverableException("Exectutable file hash calculation failed", e);
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
