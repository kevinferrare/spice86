package spice86.emulator.interrupthandlers.vga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.devices.video.VgaCard;
import spice86.emulator.errors.UnhandledOperationException;
import spice86.emulator.interrupthandlers.InterruptHandler;
import spice86.emulator.machine.Machine;
import spice86.emulator.memory.MemoryMap;
import spice86.emulator.memory.MemoryUtils;
import spice86.utils.ConvertUtils;

/**
 * Implementation of int10.<br/>
 * Currently only supports mode 0x13.<br/>
 * Displays to the GUI.<br/>
 * <ul>
 * <li>https://stanislavs.org/helppc/int_10.html</li>
 * <li>https://wiki.osdev.org/VGA_Hardware</li>
 * </ul>
 */
public class VideoBiosInt10Handler extends InterruptHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(VideoBiosInt10Handler.class);

  public static final int CRT_IO_PORT_ADDRESS_IN_RAM =
      MemoryUtils.toPhysicalAddress(MemoryMap.BIOS_DATA_AREA_SEGMENT, MemoryMap.BIOS_DATA_AREA_OFFSET_CRT_IO_PORT);

  public static final int BIOS_VIDEO_MODE = 0x49;
  public static final int BIOS_VIDEO_MODE_ADDRESS =
      MemoryUtils.toPhysicalAddress(MemoryMap.BIOS_DATA_AREA_SEGMENT, BIOS_VIDEO_MODE);

  private VgaCard vgaCard;
  private int numberOfScreenColumns = 80;
  private int currentDisplayPage = 0;

  public VideoBiosInt10Handler(Machine machine, VgaCard vgaCard) {
    super(machine);
    this.vgaCard = vgaCard;
    fillDispatchTable();
  }

  private void fillDispatchTable() {
    super.dispatchTable.put(0x00, this::setVideoMode);
    super.dispatchTable.put(0x01, this::setCursorType);
    super.dispatchTable.put(0x02, this::setCursorPosition);
    super.dispatchTable.put(0x06, this::scrollPageUp);
    super.dispatchTable.put(0x0B, this::setColorPalette);
    super.dispatchTable.put(0x0E, this::writeTextInTeletypeMode);
    super.dispatchTable.put(0x0F, this::getVideoStatus);
    super.dispatchTable.put(0x10, this::getSetPaletteRegisters);
    super.dispatchTable.put(0x12, this::videoSubsystemConfiguration);
    super.dispatchTable.put(0x1A, this::videoDisplayCombination);
  }

  public void initRam() {
    this.setVideoModeValue(VgaCard.MODE_320_200_256);
    memory.setUint16(CRT_IO_PORT_ADDRESS_IN_RAM, VgaCard.CRT_IO_PORT);
  }

  @Override
  public int getIndex() {
    return 0x10;
  }

  @Override
  public void run() throws UnhandledOperationException {
    int operation = state.getAH();
    this.run(operation);
  }

  public void setVideoMode() {
    int videoMode = state.getAL();
    setVideoModeValue(videoMode);
  }

  public void setCursorType() {
    int cursorStartEnd = state.getCX();
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("SET CURSOR TYPE, SCAN LINE START END IS {}", ConvertUtils.toHex(cursorStartEnd));
    }
  }

  public void setCursorPosition() {
    int cursorPositionRow = state.getDH();
    int cursorPositionColumn = state.getDL();
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("SET CURSOR POSITION, ROW:{}, COL:{}", ConvertUtils.toHex8(cursorPositionRow),
          ConvertUtils.toHex8(cursorPositionColumn));
    }
  }

  public void scrollPageUp() {
    int scrollAmount = state.getAL();
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("SCROLL PAGE UP BY AMOUNT {}", ConvertUtils.toHex8(scrollAmount));
    }
  }

  public void setColorPalette() {
    int colorId = state.getBH();
    int colorValue = state.getBL();
    LOGGER.info("SET COLOR PALETTE colorId:{}, colorValue:{}", colorId, colorValue);
  }

  public void writeTextInTeletypeMode() {
    int chr = state.getAL();
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Write Text in Teletype Mode ascii code {}, chr {}", ConvertUtils.toHex(chr),
          ConvertUtils.toChar(chr));
    }
  }

  public void getVideoStatus() {
    LOGGER.debug("GET VIDEO STATUS");
    state.setAH(numberOfScreenColumns);
    state.setAL(getVideoModeValue());
    state.setBH(currentDisplayPage);
  }

  public void getSetPaletteRegisters() throws UnhandledOperationException {
    int op = state.getAL();
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("GET/SET PALETTE REGISTERS operation={}", ConvertUtils.toHex8(op));
    }
    if (op == 0x12) {
      setBlockOfDacColorRegisters();
    } else if (op == 0x17) {
      getBlockOfDacColorRegisters();
    } else {
      throw new UnhandledOperationException(machine,
          "Unhandled operation for get/set palette registers op=" + ConvertUtils.toHex8(op));
    }
  }

  public void videoSubsystemConfiguration() throws UnhandledOperationException {
    int op = state.getBL();
    switch (op) {
      case 0x0 -> LOGGER.info("UNKNOWN!");
      case 0x10 -> {
        LOGGER.info("GET VIDEO CONFIGURATION INFORMATION");
        // color
        state.setBH(0);
        // 64k of vram
        state.setBL(0);
        // From dosbox source code ...
        state.setCH(0);
        state.setCL(0x09);
      }
      default -> throw new UnhandledOperationException(machine,
          "Unhandled operation for videoSubsystemConfiguration op=" + ConvertUtils.toHex8(op));
    }
  }

  public void videoDisplayCombination() throws UnhandledOperationException {
    int op = state.getAL();
    switch (op) {
      case 0 -> {
        LOGGER.info("GET VIDEO DISPLAY COMBINATION");
        // VGA with analog color display
        state.setBX(0x08);
      }
      case 1 -> {
        LOGGER.info("SET VIDEO DISPLAY COMBINATION");
        throw new UnhandledOperationException(machine, "Unimplemented");
      }
      default -> throw new UnhandledOperationException(machine,
          "Unhandled operation for videoDisplayCombination op=" + ConvertUtils.toHex8(op));
    }
    state.setAL(0x1A);
  }

  public void setBlockOfDacColorRegisters() {
    int firstRegisterToSet = state.getBX();
    int numberOfColorsToSet = state.getCX();
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info(
          "SET BLOCKS OF DAC COLOR REGISTERS. First register is {}, setting {} colors, values are from address {}",
          ConvertUtils.toHex(firstRegisterToSet),
          numberOfColorsToSet, ConvertUtils.toSegmentedAddressRepresentation(state.getES(), state.getDX()));
    }
    int colorValuesAddress = MemoryUtils.toPhysicalAddress(state.getES(), state.getDX());
    vgaCard.setBlockOfDacColorRegisters(firstRegisterToSet, numberOfColorsToSet, colorValuesAddress);
  }

  public void getBlockOfDacColorRegisters() {
    int firstRegisterToGet = state.getBX();
    int numberOfColorsToGet = state.getCX();
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info(
          "GET BLOCKS OF DAC COLOR REGISTERS. First register is {}, getting {} colors, values are to be stored at address {}",
          ConvertUtils.toHex(firstRegisterToGet),
          numberOfColorsToGet, ConvertUtils.toSegmentedAddressRepresentation(state.getES(), state.getDX()));
    }
    int colorValuesAddress = MemoryUtils.toPhysicalAddress(state.getES(), state.getDX());
    vgaCard.getBlockOfDacColorRegisters(firstRegisterToGet, numberOfColorsToGet, colorValuesAddress);
  }

  public int getVideoModeValue() {
    LOGGER.info("GET VIDEO MODE");
    return memory.getUint8(BIOS_VIDEO_MODE_ADDRESS);
  }

  public void setVideoModeValue(int mode) {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("SET VIDEO MODE {}", ConvertUtils.toHex8(mode));
    }
    memory.setUint8(BIOS_VIDEO_MODE_ADDRESS, mode);
    vgaCard.setVideoModeValue(mode);
  }
}
