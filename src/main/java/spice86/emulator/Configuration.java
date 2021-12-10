package spice86.emulator;

import spice86.emulator.function.OverrideSupplier;

/**
 * Configuration for spice86, that is what to run and how.
 */
public class Configuration {
  private String exe;
  private String cDrive;
  // Only for timer
  private Long instructionsPerSecond;
  private double timeMultiplier;
  private Integer gdbPort;
  private OverrideSupplier overrideSupplier;
  private boolean useCodeOverride;
  private boolean installInterruptVector;
  private boolean failOnUnhandledPort;
  private int programEntryPointSegment;
  private byte[] expectedChecksum = new byte[0];
  private String defaultDumpDirectory;

  public String getExe() {
    return exe;
  }

  public void setExe(String exe) {
    this.exe = exe;
  }

  public String getcDrive() {
    return cDrive;
  }

  public void setcDrive(String cDrive) {
    this.cDrive = cDrive;
  }

  public Long getInstructionsPerSecond() {
    return instructionsPerSecond;
  }

  public void setInstructionsPerSecond(Long instructionsPerSecond) {
    this.instructionsPerSecond = instructionsPerSecond;
  }

  public double getTimeMultiplier() {
    return timeMultiplier;
  }

  public void setTimeMultiplier(double timeMultiplier) {
    this.timeMultiplier = timeMultiplier;
  }

  public Integer getGdbPort() {
    return gdbPort;
  }

  public void setGdbPort(Integer gdbPort) {
    this.gdbPort = gdbPort;
  }

  public OverrideSupplier getOverrideSupplier() {
    return overrideSupplier;
  }

  public void setOverrideSupplier(OverrideSupplier overrideSupplier) {
    this.overrideSupplier = overrideSupplier;
  }

  public boolean isUseCodeOverride() {
    return useCodeOverride;
  }

  public void setUseCodeOverride(boolean useCodeOverride) {
    this.useCodeOverride = useCodeOverride;
  }

  public boolean isInstallInterruptVector() {
    return installInterruptVector;
  }

  public void setInstallInterruptVector(boolean installInterruptVector) {
    this.installInterruptVector = installInterruptVector;
  }

  public boolean isFailOnUnhandledPort() {
    return failOnUnhandledPort;
  }

  public void setFailOnUnhandledPort(boolean failOnUnhandledPort) {
    this.failOnUnhandledPort = failOnUnhandledPort;
  }

  public int getProgramEntryPointSegment() {
    return programEntryPointSegment;
  }

  public void setProgramEntryPointSegment(int programEntryPointSegment) {
    this.programEntryPointSegment = programEntryPointSegment;
  }

  public byte[] getExpectedChecksum() {
    return expectedChecksum;
  }

  public void setExpectedChecksum(byte[] expectedChecksum) {
    this.expectedChecksum = expectedChecksum;
  }

  public String getDefaultDumpDirectory() {
    return defaultDumpDirectory;
  }

  public void setDefaultDumpDirectory(String defaultDumpDirectory) {
    this.defaultDumpDirectory = defaultDumpDirectory;
  }
}
