package spice86.emulator.interrupthandlers.dos;

/**
 * Result of a DOS file operation.<br/>
 * Indicates whether an error occurred or not, and whether the value (to set in AX) is 16 or 32 bits (32 bits values
 * high bits gets into DX).<br/>
 * The static methods error, value16, value32 and noValue should be used to create an instance.
 */
public class DosFileOperationResult {
  private boolean error;
  private boolean valueIsUint32;
  private Integer value;

  public static DosFileOperationResult error(int errorCode) {
    return new DosFileOperationResult(true, false, errorCode);
  }

  public static DosFileOperationResult value16(int fileHandle) {
    return new DosFileOperationResult(false, false, fileHandle);
  }

  public static DosFileOperationResult value32(int offset) {
    return new DosFileOperationResult(false, true, offset);
  }

  public static DosFileOperationResult noValue() {
    return new DosFileOperationResult(false, false, null);
  }

  private DosFileOperationResult(boolean error, boolean valueIsUint32, Integer value) {
    this.error = error;
    this.valueIsUint32 = valueIsUint32;
    this.value = value;
  }

  public boolean isError() {
    return error;
  }

  public boolean isValueIsUint32() {
    return valueIsUint32;
  }

  public Integer getValue() {
    return value;
  }

}
