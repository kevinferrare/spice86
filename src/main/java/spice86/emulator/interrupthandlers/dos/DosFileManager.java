package spice86.emulator.interrupthandlers.dos;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.errors.UnrecoverableException;
import spice86.emulator.memory.Memory;
import spice86.emulator.memory.MemoryRange;
import spice86.emulator.memory.MemoryUtils;

public class DosFileManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(DosFileManager.class);

  public static final int FILE_HANDLE_OFFSET = 5;
  private static final int MAX_OPEN_FILES = 15;
  private static final Map<Integer, String> FILE_OPEN_MODE = new HashMap<>();
  static {
    FILE_OPEN_MODE.put(0x00, "r");
    FILE_OPEN_MODE.put(0x01, "w");
    FILE_OPEN_MODE.put(0x02, "rw");
  }
  private Memory memory;
  private OpenFile[] openFiles = new OpenFile[MAX_OPEN_FILES];
  private String currentDir;
  private Map<Character, String> driveMap;
  private int diskTransferAreaAddressSegment;
  private int diskTransferAreaAddressOffset;

  private String currentMatchingFileSearchFolder;
  private String currentMatchingFileSearchSpec;
  private Iterator<Path> matchingFilesIterator;

  public DosFileManager(Memory memory) {
    this.memory = memory;
  }

  public void setDiskTransferAreaAddress(int diskTransferAreaAddressSegment, int diskTransferAreaAddressOffset) {
    this.diskTransferAreaAddressSegment = diskTransferAreaAddressSegment;
    this.diskTransferAreaAddressOffset = diskTransferAreaAddressOffset;
  }

  public int getDiskTransferAreaAddressSegment() {
    return diskTransferAreaAddressSegment;
  }

  public int getDiskTransferAreaAddressOffset() {
    return diskTransferAreaAddressOffset;
  }

  private int getDiskTransferAreaAddressPhysical() {
    return MemoryUtils.toPhysicalAddress(diskTransferAreaAddressSegment, diskTransferAreaAddressOffset);
  }

  public void setDiskParameters(String currentDir, Map<Character, String> driveMap) {
    this.currentDir = currentDir;
    this.driveMap = driveMap;
  }

  public DosFileOperationResult setCurrentDir(String currentDir) {
    this.currentDir = toHostFileName(currentDir);
    return DosFileOperationResult.noValue();
  }

  public DosFileOperationResult createFileUsingHandle(String fileName, int fileAttribute) {
    String hostFileName = toHostFileName(fileName);
    LOGGER.info("Creating file {} with attribute {}", hostFileName, fileAttribute);

    Path path = Paths.get(hostFileName);
    try {
      if (Files.exists(path)) {
        Files.delete(path);
      }
      Files.createFile(path);
    } catch (IOException e) {
      throw new UnrecoverableException("IOException while creating file", e);
    }
    return openFileInternal(fileName, hostFileName, "rw");
  }

  public DosFileOperationResult openFile(String fileName, int accessMode) {
    String hostFileName = toHostFileName(fileName);
    String openMode = FILE_OPEN_MODE.get(accessMode);
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Opening file {} with mode {}", hostFileName, openMode);
    }
    return openFileInternal(fileName, hostFileName, openMode);
  }

  public DosFileOperationResult closeFile(int fileHandle) {

    OpenFile file = getOpenFile(fileHandle);
    if (file == null) {
      LOGGER.warn("File not opened: {}", fileHandle);
      return DosFileOperationResult.error(0x06);
    }
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Closed {}, file was loaded in ram in those addresses: {}", file.getName(),
          file.getLoadMemoryRanges());
    }
    setOpenFile(fileHandle, null);
    try {
      file.getRandomAccessFile().close();
    } catch (IOException e) {
      throw new UnrecoverableException("IOException while closing file", e);
    }
    return DosFileOperationResult.noValue();
  }

  public DosFileOperationResult readFile(int fileHandle, int readLength, int targetAddress) {
    OpenFile file = getOpenFile(fileHandle);
    if (file == null) {
      return handleFileNotOpenedError(fileHandle);
    }
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Reading from file {}", file.getName());
    }
    byte[] buffer = new byte[readLength];
    int actualReadLength;
    try {
      actualReadLength = file.getRandomAccessFile().read(buffer, 0, readLength);
    } catch (IOException e) {
      throw new UnrecoverableException("IOException while reading file", e);
    }
    if (actualReadLength == -1) {
      // EOF
      return DosFileOperationResult.value16(0);
    }
    memory.loadData(targetAddress, buffer, actualReadLength);
    file.addMemoryRange(new MemoryRange(targetAddress, targetAddress + actualReadLength, file.getName()));
    return DosFileOperationResult.value16(actualReadLength);
  }

  public DosFileOperationResult writeFileUsingHandle(int fileHandle, int writeLength, int bufferAddress) {
    if (!isValidFileHandle(fileHandle)) {
      LOGGER.warn("Invalid or unsupported file handle {}. Doing nothing.", fileHandle);
      // Fake that we wrote, this could be used to write to stdout / stderr ...
      return DosFileOperationResult.value16(writeLength);
    }
    OpenFile file = getOpenFile(fileHandle);
    if (file == null) {
      return handleFileNotOpenedError(fileHandle);
    }
    try {
      file.getRandomAccessFile().write(memory.getRam(), bufferAddress, writeLength);
    } catch (IOException e) {
      throw new UnrecoverableException("IOException while writing file", e);
    }
    return DosFileOperationResult.value16(writeLength);
  }

  public DosFileOperationResult moveFilePointerUsingHandle(int originOfMove, int fileHandle, int offset) {
    OpenFile file = getOpenFile(fileHandle);
    if (file == null) {
      return handleFileNotOpenedError(fileHandle);
    }
    LOGGER.info("Moving in file {}", file.getName());
    RandomAccessFile randomAccessFile = file.getRandomAccessFile();

    try {
      int newOffset = seek(randomAccessFile, originOfMove, offset);
      return DosFileOperationResult.value32(newOffset);
    } catch (IOException e) {
      LOGGER.error("An error occurred while seeking file", e);
      return DosFileOperationResult.error(0x19);
    }
  }

  public DosFileOperationResult findFirstMatchingFile(String fileSpec) {
    String hostSearchSpec = toHostFileName(fileSpec);
    currentMatchingFileSearchFolder = hostSearchSpec.substring(0, hostSearchSpec.lastIndexOf('/') + 1);
    currentMatchingFileSearchSpec = hostSearchSpec.replace(currentMatchingFileSearchFolder, "").toLowerCase();
    try (Stream<Path> pathes = Files.walk(Paths.get(currentMatchingFileSearchFolder))) {
      List<Path> matchingPathes = pathes
          .filter(p -> matchesSpec(currentMatchingFileSearchSpec, p))
          .toList();
      matchingFilesIterator = matchingPathes.iterator();
      return findNextMatchingFile();
    } catch (IOException e) {
      LOGGER.warn("Error while walking path {} or getting attributes.", currentMatchingFileSearchFolder);
      return DosFileOperationResult.error(0x03);
    }
  }

  public DosFileOperationResult findNextMatchingFile() {
    if (matchingFilesIterator == null) {
      LOGGER.warn("No search was done");
      return DosFileOperationResult.error(0x02);
    }
    if (!matchingFilesIterator.hasNext()) {
      LOGGER.warn("No more files matching {} in path {}", currentMatchingFileSearchSpec,
          currentMatchingFileSearchFolder);
      return DosFileOperationResult.error(0x02);
    }
    Path matching = matchingFilesIterator.next();
    try {
      updateDTAFromFile(matching);
    } catch (IOException e) {
      LOGGER.warn("Error while getting attributes.");
      return DosFileOperationResult.error(0x02);
    }
    return DosFileOperationResult.noValue();
  }

  /**
   * @param fileSpec
   *          a filename with ? when any character can match
   * @param item
   * @return
   */
  private boolean matchesSpec(String fileSpec, Path item) {
    if (Files.isDirectory(item)) {
      return false;
    }
    String fileName = item.getFileName().toString().toLowerCase();
    if (fileSpec.length() != fileName.length()) {
      return false;
    }
    for (int i = 0; i < fileSpec.length(); i++) {
      char fileSpecChar = fileSpec.charAt(i);
      if (fileSpecChar == '?') {
        continue;
      }
      char fileNameChar = fileName.charAt(i);
      if (fileNameChar != fileSpecChar) {
        return false;
      }
    }
    return true;
  }

  private void updateDTAFromFile(Path matchingFile) throws IOException {
    LOGGER.info("Found matching file {}", matchingFile);
    DosDiskTransferArea dosDiskTransferArea =
        new DosDiskTransferArea(this.memory, this.getDiskTransferAreaAddressPhysical());
    BasicFileAttributes attributes = Files.readAttributes(matchingFile, BasicFileAttributes.class);
    ZonedDateTime creationZonedDateTime = attributes.creationTime().toInstant().atZone(ZoneOffset.UTC);
    LocalDate creationLocalDate = creationZonedDateTime.toLocalDate();
    LocalTime creationLocalTime = creationZonedDateTime.toLocalTime();

    dosDiskTransferArea.setFileDate(toDosDate(creationLocalDate));
    dosDiskTransferArea.setFileTime(toDosTime(creationLocalTime));
    dosDiskTransferArea.setFileSize((int)attributes.size());
    dosDiskTransferArea.setFileName(matchingFile.getFileName().toString());
  }

  private int toDosDate(LocalDate localDate) {
    // https://stanislavs.org/helppc/file_attributes.html
    int day = localDate.getDayOfMonth();
    int month = localDate.getMonthValue();
    int dosYear = localDate.getYear() - 1980;
    return (day & 0b11111) | ((month & 0b1111) << 5) | ((dosYear & 0b1111111) << 9);
  }

  private int toDosTime(LocalTime localTime) {
    // https://stanislavs.org/helppc/file_attributes.html
    int dosSeconds = localTime.getSecond() / 2;
    int minutes = localTime.getMinute();
    int hours = localTime.getHour();
    return (dosSeconds & 0b11111) | ((minutes & 0b111111) << 5) | ((hours & 0b11111) << 11);
  }

  private int seek(RandomAccessFile randomAccessFile, int originOfMove, int offset) throws IOException {
    long newOffset;
    if (originOfMove == 0) {
      newOffset = offset;
      // seek from beginning, offset is good
    } else if (originOfMove == 1) {
      // seek from last read
      newOffset = randomAccessFile.getFilePointer() + offset;
    } else {
      // seek from end
      newOffset = randomAccessFile.length() - offset;
    }
    randomAccessFile.seek(newOffset);
    return (int)newOffset;
  }

  private DosFileOperationResult handleFileNotOpenedError(int fileHandle) {
    LOGGER.warn("File not opened: {}", fileHandle);
    return DosFileOperationResult.error(0x06);
  }

  private OpenFile getOpenFile(int fileHandle) {
    return openFiles[fileHandleToIndex(fileHandle)];
  }

  private void setOpenFile(int fileHandle, OpenFile openFile) {
    openFiles[fileHandleToIndex(fileHandle)] = openFile;
  }

  private DosFileOperationResult openFileInternal(String fileName, String hostFileName, String openMode) {
    Integer freeIndex = findNextFreeFileIndex();
    if (freeIndex == null) {
      LOGGER.warn("Could not find a free handle");
      return DosFileOperationResult.error(0x04);
    }
    int dosIndex = freeIndex + FILE_HANDLE_OFFSET;
    try {
      RandomAccessFile randomAccessFile = new RandomAccessFile(hostFileName, openMode);
      setOpenFile(dosIndex, new OpenFile(fileName, dosIndex, randomAccessFile));
    } catch (FileNotFoundException fne) {
      return DosFileOperationResult.error(0x02);
    }
    return DosFileOperationResult.value16(dosIndex);
  }

  private boolean isValidFileHandle(int fileHandle) {
    return fileHandle >= FILE_HANDLE_OFFSET && fileHandle <= MAX_OPEN_FILES + FILE_HANDLE_OFFSET;
  }

  private int fileHandleToIndex(int fileHandle) {
    return fileHandle - FILE_HANDLE_OFFSET;
  }

  private Integer findNextFreeFileIndex() {
    for (int i = 0; i < openFiles.length; i++) {
      if (openFiles[i] == null) {
        return i;
      }
    }
    return null;
  }

  private String toHostFileName(String dosFileName) {
    String fileName = dosFileName.replace('\\', '/');
    if (fileName.length() >= 2 && fileName.charAt(1) == ':') {
      fileName = replaceDriveWithHostPath(fileName);
    } else {
      // Same as the exe, prefix with currentDir
      fileName = currentDir + fileName;
    }
    return fileName.replace("//", "/");
  }

  private String replaceDriveWithHostPath(String fileName) {
    // Absolute path
    char driveLetter = fileName.charAt(0);
    String pathForDrive = driveMap.get(driveLetter);
    if (pathForDrive == null) {
      throw new UnrecoverableException("Could not find a mapping for drive " + driveLetter);
    }
    return fileName.replace(driveLetter + ":", pathForDrive);
  }

}
