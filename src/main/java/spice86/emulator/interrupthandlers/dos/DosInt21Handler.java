package spice86.emulator.interrupthandlers.dos;

import java.io.File;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.errors.UnhandledOperationException;
import spice86.emulator.interrupthandlers.InterruptHandler;
import spice86.emulator.machine.Machine;
import spice86.emulator.memory.Memory;
import spice86.emulator.memory.MemoryUtils;
import spice86.utils.ConvertUtils;

/**
 * Reimplementation of int21
 */
@SuppressWarnings({
    // Some switches do not have a lot of case statements, that's because the rest is not implemented (yet).
    "java:S1301",
})
public class DosInt21Handler extends InterruptHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(DosInt21Handler.class);

  private static final Charset CP850_CHARSET = Charset.forName("CP850");

  private boolean ctrlCFlag = false;
  // dosbox
  private int defaultDrive = 2;
  private DosMemoryManager dosMemoryManager;
  private DosFileManager dosFileManager;
  private StringBuilder displayOutputBuilder = new StringBuilder();

  public DosInt21Handler(Machine machine) {
    super(machine);
    dosMemoryManager = new DosMemoryManager(machine.getMemory());
    dosFileManager = new DosFileManager(memory);
    fillDispatchTable();
  }

  public DosMemoryManager getDosMemoryManager() {
    return dosMemoryManager;
  }

  public DosFileManager getDosFileManager() {
    return dosFileManager;
  }

  private void fillDispatchTable() {
    super.dispatchTable.put(0x02, this::displayOutput);
    super.dispatchTable.put(0x06, () -> directConsoleIo(true));
    super.dispatchTable.put(0x09, this::printString);
    super.dispatchTable.put(0x0C, this::clearKeyboardBufferAndInvokeKeyboardFunction);
    super.dispatchTable.put(0x0E, this::selectDefaultDrive);
    super.dispatchTable.put(0x1A, this::setDiskTransferAddress);
    super.dispatchTable.put(0x19, this::getCurrentDefaultDrive);
    super.dispatchTable.put(0x25, this::setInterruptVector);
    super.dispatchTable.put(0x2A, this::getDate);
    super.dispatchTable.put(0x2C, this::getTime);
    super.dispatchTable.put(0x2F, this::getDiskTransferAddress);
    super.dispatchTable.put(0x30, this::getDosVersion);
    super.dispatchTable.put(0x33, this::getSetControlBreak);
    super.dispatchTable.put(0x35, this::getInterruptVector);
    super.dispatchTable.put(0x36, this::getFreeDiskSpace);
    super.dispatchTable.put(0x3B, () -> changeCurrentDirectory(true));
    super.dispatchTable.put(0x3C, () -> createFileUsingHandle(true));
    super.dispatchTable.put(0x3D, () -> openFile(true));
    super.dispatchTable.put(0x3E, () -> closeFile(true));
    super.dispatchTable.put(0x3F, () -> readFile(true));
    super.dispatchTable.put(0x40, () -> writeFileUsingHandle(true));
    super.dispatchTable.put(0x43, () -> getSetFileAttribute(true));
    super.dispatchTable.put(0x44, () -> ioControl(true));
    super.dispatchTable.put(0x42, () -> moveFilePointerUsingHandle(true));
    super.dispatchTable.put(0x45, () -> duplicateFileHandle(true));
    super.dispatchTable.put(0x47, () -> getCurrentDirectory(true));
    super.dispatchTable.put(0x48, () -> allocateMemoryBlock(true));
    super.dispatchTable.put(0x49, () -> freeMemoryBlock(true));
    super.dispatchTable.put(0x4A, () -> modifyMemoryBlock(true));
    super.dispatchTable.put(0x4C, this::quitWithExitCode);
    super.dispatchTable.put(0x4E, () -> findFirstMatchingFile(true));
    super.dispatchTable.put(0x4F, () -> findNextMatchingFile(true));
  }

  @Override
  public int getIndex() {
    return 0x21;
  }

  @Override
  public void run() throws UnhandledOperationException {
    int operation = ConvertUtils.uint8(state.getAH());
    this.run(operation);
  }

  public void directConsoleIo(boolean calledFromVm) {
    int character = state.getDL();
    if (character == 0xFF) {
      LOGGER.debug("DIRECT CONSOLE IO, INPUT REQUESTED");
      // Read from STDIN, not implemented, return no character ready
      Integer scancode = machine.getKeyboardInt16Handler().getNextKeyCode();
      if (scancode == null) {
        setZeroFlag(true, calledFromVm);
        state.setAL(0);
      } else {
        int ascii = ConvertUtils.uint8(scancode);
        setZeroFlag(false, calledFromVm);
        state.setAL(ascii);
      }
    } else {
      // Output
      LOGGER.info("DIRECT CONSOLE IO, character={}, ascii={}", character, ConvertUtils.toChar(character));
    }
  }

  public void displayOutput() {
    int characterByte = state.getDL();
    String character = convertDosChar(characterByte);
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("PRINT CHR: {} ({})", ConvertUtils.toHex8(characterByte), character);
    }
    if (characterByte == '\r') {
      LOGGER.info("PRINT CHR LINE BREAK: {}", displayOutputBuilder);
      displayOutputBuilder = new StringBuilder();
    } else if (characterByte != '\n') {
      displayOutputBuilder.append(character);
    }
  }

  public void printString() {
    String string = getDosString(memory, state.getDS(), state.getDX(), '$');
    LOGGER.info("PRINT STRING: {}", string);
  }

  public void clearKeyboardBufferAndInvokeKeyboardFunction() throws UnhandledOperationException {
    int operation = state.getAL();
    LOGGER.info("CLEAR KEYBOARD AND CALL INT 21 {}", operation);
    this.run(operation);
  }

  public void selectDefaultDrive() {
    defaultDrive = state.getDL();
    LOGGER.info("SELECT DEFAULT DRIVE {}", defaultDrive);
    // Number of valid drive letters
    state.setAL(26);
  }

  public void setDiskTransferAddress() {
    int segment = state.getDS();
    int offset = state.getDX();
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("SET DTA (DISK TRANSFER ADDRESS) DS:DX {}",
          ConvertUtils.toSegmentedAddressRepresentation(segment, offset));
    }
    dosFileManager.setDiskTransferAreaAddress(segment, offset);
  }

  public void getCurrentDefaultDrive() {
    LOGGER.info("GET CURRENT DEFAULT DRIVE");
    state.setAL(defaultDrive);
  }

  public void setInterruptVector() {
    int vectorNumber = state.getAL();
    int segment = state.getDS();
    int offset = state.getDX();
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("SET INTERRUPT VECTOR FOR INT {} at address {}", ConvertUtils.toHex(vectorNumber),
          ConvertUtils.toSegmentedAddressRepresentation(segment, offset));
    }
    setInterruptVector(vectorNumber, segment, offset);
  }

  public void getDate() {
    LOGGER.info("GET DATE");
    LocalDate now = LocalDate.now();
    // in java days are from 1 to 7 (1 is monday, 7 is sunday), but for dos 0 is sunday and 1 is monday
    int dayOfWeek = now.getDayOfWeek().getValue() % 6;
    state.setAL(dayOfWeek);
    state.setCX(now.getYear());
    state.setDH(now.getMonthValue());
    state.setDL(now.getDayOfMonth());
  }

  public void getTime() {
    LOGGER.info("GET TIME");
    LocalTime now = LocalTime.now();
    state.setCH(now.getHour());
    state.setCL(now.getMinute());
    state.setDH(now.getSecond());
    state.setDL(now.getNano() / 10_000_000);
  }

  public void getDiskTransferAddress() {
    state.setES(dosFileManager.getDiskTransferAreaAddressSegment());
    state.setBX(dosFileManager.getDiskTransferAreaAddressOffset());
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("GET DTA (DISK TRANSFER ADDRESS) DS:DX {}",
          ConvertUtils.toSegmentedAddressRepresentation(state.getES(), state.getBX()));
    }
  }

  public void setInterruptVector(int vectorNumber, int segment, int offset) {
    memory.setUint16(4 * vectorNumber + 2, segment);
    memory.setUint16(4 * vectorNumber, offset);
  }

  public void getDosVersion() {
    LOGGER.info("GET DOS VERSION");
    // 5.0
    state.setAL(0x05);
    state.setAH(0x00);
    // FF => MS DOS
    state.setBH(0xFF);
    // DOS OEM KEY 0x00000
    state.setBL(0x00);
    state.setCX(0x00);
  }

  public void getSetControlBreak() throws UnhandledOperationException {
    LOGGER.info("GET/SET CTRL-C FLAG");
    int op = state.getAL();
    if (op == 0) {
      // GET
      state.setDL(ctrlCFlag ? 1 : 0);
    } else if (op == 1 || op == 2) {
      // SET
      ctrlCFlag = state.getDL() == 1;
    } else {
      throw new UnhandledOperationException(machine, "Ctrl-C get/set operation unhandled: " + op);
    }

  }

  public void getInterruptVector() {
    int vectorNumber = state.getAL();
    int segment = memory.getUint16(4 * vectorNumber + 2);
    int offset = memory.getUint16(4 * vectorNumber);
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("GET INTERRUPT VECTOR INT {}, got {}", ConvertUtils.toHex8(vectorNumber),
          ConvertUtils.toSegmentedAddressRepresentation(segment, offset));
    }
    state.setES(segment);
    state.setBX(offset);
  }

  public void getFreeDiskSpace() {
    int driveNumber = state.getDL();
    LOGGER.info("GET FREE DISK SPACE FOR DRIVE {}", driveNumber);
    // 127 sectors per cluster
    state.setAX(127);
    // 512 bytes per sector
    state.setCX(512);
    // 4096 clusters available (~250MB)
    state.setBX(4096);
    // 8192 total clusters on disk (~500MB)
    state.setDX(8192);
  }

  public void changeCurrentDirectory(boolean calledFromVm) {
    String newDirectory = getStringAtDsDx();
    LOGGER.info("SET CURRENT DIRECTORY: {}", newDirectory);
    DosFileOperationResult dosFileOperationResult = dosFileManager.setCurrentDir(newDirectory);
    setStateFromDosFileOperationResult(calledFromVm, dosFileOperationResult);
  }

  public void createFileUsingHandle(boolean calledFromVm) {
    String fileName = getStringAtDsDx();
    int fileAttribute = state.getCX();
    LOGGER.info("CREATE FILE USING HANDLE: {} with attribute {}", fileName, fileAttribute);
    DosFileOperationResult dosFileOperationResult = dosFileManager.createFileUsingHandle(fileName, fileAttribute);
    setStateFromDosFileOperationResult(calledFromVm, dosFileOperationResult);
  }

  public void openFile(boolean calledFromVm) {
    String fileName = getStringAtDsDx();
    int accessMode = state.getAL();
    int rwAccessMode = accessMode & 0b111;
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("OPEN FILE {} with mode {} (rwAccessMode:{})", fileName, ConvertUtils.toHex8(accessMode),
          ConvertUtils.toHex8(rwAccessMode));
    }
    DosFileOperationResult dosFileOperationResult = dosFileManager.openFile(fileName, rwAccessMode);
    setStateFromDosFileOperationResult(calledFromVm, dosFileOperationResult);
  }

  private void setStateFromDosFileOperationResult(boolean calledFromVm,
      DosFileOperationResult dosFileOperationResult) {
    Integer value = dosFileOperationResult.getValue();
    if (dosFileOperationResult.isError()) {
      logDosError(calledFromVm);
      setCarryFlag(true, calledFromVm);
      state.setAX(value);
    } else {
      setCarryFlag(false, calledFromVm);
    }
    if (value != null) {
      state.setAX(value);
      if (dosFileOperationResult.isValueIsUint32()) {
        state.setDX(value >>> 16);
      }
    }
  }

  public void closeFile(boolean calledFromVm) {
    int fileHandle = state.getBX();
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("CLOSE FILE handle {}", ConvertUtils.toHex(fileHandle));
    }
    DosFileOperationResult dosFileOperationResult = dosFileManager.closeFile(fileHandle);
    setStateFromDosFileOperationResult(calledFromVm, dosFileOperationResult);
  }

  public void readFile(boolean calledFromVm) {
    int fileHandle = state.getBX();
    int readLength = state.getCX();
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("READ FROM FILE handle {} length {} to {}", fileHandle, readLength,
          ConvertUtils.toSegmentedAddressRepresentation(state.getDS(), state.getDX()));
    }
    int targetMemory = MemoryUtils.toPhysicalAddress(state.getDS(), state.getDX());
    DosFileOperationResult dosFileOperationResult = dosFileManager.readFile(fileHandle, readLength, targetMemory);
    setStateFromDosFileOperationResult(calledFromVm, dosFileOperationResult);
  }

  public void writeFileUsingHandle(boolean calledFromVm) {
    int fileHandle = state.getBX();
    int writeLength = state.getCX();
    int bufferAddress = MemoryUtils.toPhysicalAddress(state.getDS(), state.getDX());
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("WRITE TO FILE handle {} length {} from {}", ConvertUtils.toHex(fileHandle),
          ConvertUtils.toHex(writeLength), ConvertUtils.toSegmentedAddressRepresentation(state.getDS(), state.getDX()));
    }
    DosFileOperationResult dosFileOperationResult =
        dosFileManager.writeFileUsingHandle(fileHandle, writeLength, bufferAddress);
    setStateFromDosFileOperationResult(calledFromVm, dosFileOperationResult);
  }

  private void getSetFileAttribute(boolean calledFromVm) throws UnhandledOperationException {
    int op = state.getAL();
    String fileName = getStringAtDsDx();
    File file = new File(fileName);
    if (!file.exists()) {
      logDosError(calledFromVm);
      setCarryFlag(true, calledFromVm);
      // File not found
      state.setAX(0x2);
      return;
    }
    setCarryFlag(false, calledFromVm);
    switch (op) {
      case 0 -> {
        LOGGER.info("GET FILE ATTRIBUTE file={}", fileName);
        // let's always return the file is read / write
        state.setCX(file.canWrite() ? 0 : 1);
      }
      case 1 -> {
        int attribute = state.getCX();
        LOGGER.info("SET FILE ATTRIBUTE file={} attribute={}", fileName, attribute);
      }
      default -> throw new UnhandledOperationException(machine, "getSetFileAttribute operation unhandled: " + op);
    }
  }

  private void ioControl(boolean calledFromVm) throws UnhandledOperationException {
    int op = state.getAL();
    int device = state.getBX();
    setCarryFlag(false, calledFromVm);
    switch (op) {
      case 0 -> {
        LOGGER.info("GET DEVICE INFORMATION");
        // Character or block device?
        int res = device < DosFileManager.FILE_HANDLE_OFFSET ? 0x80D3 : 0x02;
        state.setDX(res);
      }
      case 1 -> LOGGER.info("SET DEVICE INFORMATION (unimplemented)");
      case 0xE -> {
        int driveNumber = state.getBL();
        LOGGER.info("GET LOGICAL DRIVE FOR PHYSICAL DRIVE {}", driveNumber);
        // Only one drive
        state.setAL(0);
      }
      default -> throw new UnhandledOperationException(machine, "IO Control operation unhandled: " + op);
    }
  }

  public void moveFilePointerUsingHandle(boolean calledFromVm) {
    int originOfMove = state.getAL();
    int fileHandle = state.getBX();
    int offset = state.getCX() << 16 | state.getDX();
    LOGGER.info("MOVE FILE POINTER USING HANDLE. originOfMove={}, fileHandle={}, offset={}", originOfMove, fileHandle,
        offset);

    DosFileOperationResult dosFileOperationResult =
        dosFileManager.moveFilePointerUsingHandle(originOfMove, fileHandle, offset);
    setStateFromDosFileOperationResult(calledFromVm, dosFileOperationResult);
  }

  public void duplicateFileHandle(boolean calledFromVm) {
    int fileHandle = state.getBX();
    LOGGER.info("DUPLICATE FILE HANDLE. fileHandle={}", fileHandle);
    DosFileOperationResult dosFileOperationResult = dosFileManager.duplicateFileHandle(fileHandle);
    setStateFromDosFileOperationResult(calledFromVm, dosFileOperationResult);
  }

  private void getCurrentDirectory(boolean calledFromVm) {
    setCarryFlag(false, calledFromVm);
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("GET CURRENT DIRECTORY responseAddress={}",
          ConvertUtils.toSegmentedAddressRepresentation(state.getDS(), state.getSI()));
    }
    int responseAddress = MemoryUtils.toPhysicalAddress(state.getDS(), state.getSI());
    // Fake that we are always at the root of the drive (empty String)
    memory.setUint8(responseAddress, 0);
  }

  public void allocateMemoryBlock(boolean calledFromVm) {
    int requestedSize = state.getBX();
    LOGGER.info("ALLOCATE MEMORY BLOCK size={}", requestedSize);
    setCarryFlag(false, calledFromVm);
    DosMemoryControlBlock res = dosMemoryManager.allocateMemoryBlock(requestedSize);
    if (res == null) {
      logDosError(calledFromVm);
      // did not find something good, error
      setCarryFlag(true, calledFromVm);
      DosMemoryControlBlock largest = dosMemoryManager.findLargestFree();
      // INSUFFICIENT MEMORY
      state.setAX(0x08);
      if (largest != null) {
        state.setBX(largest.getSize());
      } else {
        state.setBX(0);
      }
      return;
    }
    state.setAX(res.getUseableSpaceSegment());
  }

  public void freeMemoryBlock(boolean calledFromVm) {
    int blockSegment = state.getES();
    LOGGER.info("FREE ALLOCATED MEMORY blockSegment={}", blockSegment);
    setCarryFlag(false, calledFromVm);
    if (!dosMemoryManager.freeMemoryBlock(blockSegment - 1)) {
      logDosError(calledFromVm);
      setCarryFlag(true, calledFromVm);
      // INVALID MEMORY BLOCK ADDRESS
      state.setAX(0x09);
    }

  }

  public void modifyMemoryBlock(boolean calledFromVm) {
    int requestedSize = state.getBX();
    int blockSegment = state.getES();
    LOGGER.info("MODIFY MEMORY BLOCK size={}, blockSegment={}", requestedSize, blockSegment);
    setCarryFlag(false, calledFromVm);
    if (!dosMemoryManager.modifyBlock(blockSegment - 1, requestedSize)) {
      logDosError(calledFromVm);
      // An error occurred. Report it as not enough memory.
      setCarryFlag(true, calledFromVm);
      state.setAX(0x08);
      state.setBX(0);
    }
  }

  public void quitWithExitCode() {
    int exitCode = state.getAL();
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("QUIT WITH EXIT CODE {}", ConvertUtils.toHex8(exitCode));
    }
    cpu.setRunning(false);
  }

  public void findFirstMatchingFile(boolean calledFromVm) {
    int attributes = state.getCX();
    String fileSpec = getStringAtDsDx();
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("FIND FIRST MATCHING FILE attributes={}, fileSpec={}", ConvertUtils.toHex16(attributes), fileSpec);
    }
    DosFileOperationResult dosFileOperationResult =
        dosFileManager.findFirstMatchingFile(fileSpec);
    setStateFromDosFileOperationResult(calledFromVm, dosFileOperationResult);
  }

  public void findNextMatchingFile(boolean calledFromVm) {
    int attributes = state.getCX();
    String fileSpec = getStringAtDsDx();
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("FIND NEXT MATCHING FILE attributes={}, fileSpec={}", ConvertUtils.toHex16(attributes), fileSpec);
    }
    DosFileOperationResult dosFileOperationResult =
        dosFileManager.findNextMatchingFile();
    setStateFromDosFileOperationResult(calledFromVm, dosFileOperationResult);
  }

  private String getStringAtDsDx() {
    return getDosString(memory, state.getDS(), state.getDX(), '\0');
  }

  private String getDosString(Memory memory, int segment, int offset, char end) {
    int stringStart = MemoryUtils.toPhysicalAddress(segment, offset);
    StringBuilder stringBuilder = new StringBuilder();
    while (memory.getUint8(stringStart) != end) {
      String c = convertDosChar(memory.getUint8(stringStart++));
      stringBuilder.append(c);
    }
    return stringBuilder.toString();
  }

  private String convertDosChar(int characterByte) {
    return new String(new byte[] { (byte)characterByte }, CP850_CHARSET);
  }

  private void logDosError(boolean calledFromVm) {
    String returnMessage = "";
    if (calledFromVm) {
      returnMessage = "Int will return to " + machine.peekReturn() + ". ";
    }
    LOGGER.error("DOS operation failed with an error. {}State is {}", returnMessage, state);
  }
}
