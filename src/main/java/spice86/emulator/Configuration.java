package spice86.emulator;

import spice86.emulator.function.OverrideSupplier;

/**
 * Configuration for spice86, that is what to run and how.
 */
public class Configuration {
  private String exe;
  private String cDrive;
  // Only for timer
  private long instructionsPerSecond;
  private Integer gdbPort;
  private OverrideSupplier overrideSupplier;
  private boolean useCodeOverride;
  private boolean installInterruptVector;
  private int programEntryPointSegment;

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

  public long getInstructionsPerSecond() {
    return instructionsPerSecond;
  }

  public void setInstructionsPerSecond(long instructionsPerSecond) {
    this.instructionsPerSecond = instructionsPerSecond;
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

  public int getProgramEntryPointSegment() {
    return programEntryPointSegment;
  }

  public void setProgramEntryPointSegment(int programEntryPointSegment) {
    this.programEntryPointSegment = programEntryPointSegment;
  }
}
