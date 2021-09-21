package spice86.emulator.devices.video;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.errors.InvalidOperationException;
import spice86.emulator.ioports.AllUnhandledIOPortHandler;
import spice86.emulator.ioports.IOPortDispatcher;
import spice86.emulator.ioports.UnhandledIOPortException;
import spice86.emulator.machine.Machine;
import spice86.emulator.memory.MemoryMap;
import spice86.emulator.memory.MemoryUtils;
import spice86.ui.Gui;

/**
 * Implementation of VGA card, currently only supports mode 0x13.<br/>
 */
public class VgaCard extends AllUnhandledIOPortHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(VgaCard.class);

  public static final int CRT_IO_PORT = 0x03D4;
  // http://www.osdever.net/FreeVGA/vga/extreg.htm#3xAR
  public static final int VGA_SEQUENCER_ADDRESS_REGISTER_PORT = 0x03C4;
  public static final int VGA_SEQUENCER_DATA_REGISTER_PORT = 0x03C5;
  public static final int VGA_READ_INDEX_PORT = 0x03C7;
  public static final int VGA_WRITE_INDEX_PORT = 0x03C8;
  public static final int VGA_RGB_DATA_PORT = 0x3C9;
  public static final int GRAPHICS_ADDRESS_REGISTER_PORT = 0x3CE;
  public static final int VGA_STATUS_REGISTER_PORT = 0x03DA;

  public static final int MODE_320_200_256 = 0x13;

  private Gui gui;
  private VgaDac vgaDac;
  private byte crtStatusRegister;
  private boolean drawing = false;

  public VgaCard(Machine machine, Gui gui) {
    super(machine);
    this.gui = gui;
    this.vgaDac = new VgaDac(machine);
  }

  /**
   * @return true when in retrace
   */
  public boolean tickRetrace() {
    if (drawing) {
      // Means the CRT is busy drawing a line, tells the program it should not draw
      updateScreen();
      crtStatusRegister = 0;
      drawing = false;
    } else {
      // 4th bit is 1 when the CRT finished drawing and is returning to the beginning
      // of the screen (retrace).
      // Programs use this to know if it is safe to write to VRAM.
      // They write to VRAM when this bit is set, but only after waiting for a 0
      // first.
      // This is to be sure to catch the start of the retrace to ensure having the
      // whole duration of the retrace to write to VRAM.
      // More info here: http://atrevida.comprenica.com/atrtut10.html
      drawing = true;
      crtStatusRegister = 0b1000;
    }
    return drawing;
  }

  public int getStatusRegisterPort() {
    LOGGER.info("CHECKING RETRACE");
    tickRetrace();
    return crtStatusRegister;
  }

  public int getVgaReadIndex() {
    LOGGER.info("GET VGA READ INDEX");
    return vgaDac.getState() == VgaDac.VGA_DAC_WRITE ? 0x3 : 0x0;
  }

  public void setVgaReadIndex(int value) {
    LOGGER.info("SET VGA READ INDEX {}", value);
    vgaDac.setReadIndex(value);
    vgaDac.setColour(0);
    vgaDac.setState(VgaDac.VGA_DAC_READ);
  }

  public void setVgaWriteIndex(int value) {
    LOGGER.info("SET VGA WRITE INDEX {}", value);
    vgaDac.setWriteIndex(value);
    vgaDac.setColour(0);
    vgaDac.setState(VgaDac.VGA_DAC_WRITE);
  }

  public int rgbDataRead() throws InvalidColorIndexException {
    LOGGER.info("PALETTE READ");
    return VgaDac.from8bitTo6bitColor(vgaDac.readColor());
  }

  public void rgbDataWrite(int value) throws InvalidColorIndexException {
    LOGGER.info("PALETTE WRITE {}", value);
    vgaDac.writeColor(VgaDac.from6bitColorTo8bit(value));
  }

  public void updateScreen() {
    if (gui != null) {
      gui.draw(memory.getRam(), MemoryUtils.toPhysicalAddress(MemoryMap.GRAPHIC_VIDEO_MEMORY_SEGMENT, 0),
          vgaDac.getRgbs());
    }
  }

  public void setBlockOfDacColorRegisters(int firstRegisterToSet, int numberOfColorsToSet, int colorValuesAddress) {
    Rgb[] rgbs = vgaDac.getRgbs();
    for (int i = 0; i < numberOfColorsToSet; i++) {
      int registerToSet = firstRegisterToSet + i;
      Rgb rgb = rgbs[registerToSet];
      rgb.setR(VgaDac.from6bitColorTo8bit(memory.getUint8(colorValuesAddress++)));
      rgb.setG(VgaDac.from6bitColorTo8bit(memory.getUint8(colorValuesAddress++)));
      rgb.setB(VgaDac.from6bitColorTo8bit(memory.getUint8(colorValuesAddress++)));
    }
  }

  public void setVideoModeValue(int mode) {
    if (mode == MODE_320_200_256) {
      int videoHeight = 200;
      int videoWidth = 320;
      if (gui != null) {
        gui.setResolution(videoWidth, videoHeight);
      }
    } else {
      LOGGER.error("UNSUPPORTED VIDEO MODE {}", mode);
    }
  }

  @Override
  public void initPortHandlers(IOPortDispatcher ioPortDispatcher) {
    ioPortDispatcher.addIOPortHandler(VGA_SEQUENCER_ADDRESS_REGISTER_PORT, this);
    ioPortDispatcher.addIOPortHandler(VGA_SEQUENCER_DATA_REGISTER_PORT, this);
    ioPortDispatcher.addIOPortHandler(VGA_READ_INDEX_PORT, this);
    ioPortDispatcher.addIOPortHandler(VGA_WRITE_INDEX_PORT, this);
    ioPortDispatcher.addIOPortHandler(VGA_RGB_DATA_PORT, this);
    ioPortDispatcher.addIOPortHandler(GRAPHICS_ADDRESS_REGISTER_PORT, this);
    ioPortDispatcher.addIOPortHandler(VGA_STATUS_REGISTER_PORT, this);
  }

  @Override
  public int inb(int port) throws InvalidOperationException {
    if (port == VGA_READ_INDEX_PORT) {
      return getVgaReadIndex();
    } else if (port == VGA_STATUS_REGISTER_PORT) {
      return getStatusRegisterPort();
    } else if (port == VGA_RGB_DATA_PORT) {
      return rgbDataRead();
    }
    throw new UnhandledIOPortException(machine, port);
  }

  @Override
  public void outb(int port, int value) throws InvalidOperationException {
    if (port == VGA_READ_INDEX_PORT) {
      setVgaReadIndex(value);
    } else if (port == VGA_WRITE_INDEX_PORT) {
      setVgaWriteIndex(value);
    } else if (port == VGA_RGB_DATA_PORT) {
      rgbDataWrite(value);
    } else if (port == VGA_STATUS_REGISTER_PORT) {
      boolean vsync = (value & 0b100) != 1;
      LOGGER.info("Vsync value set to {} (this is not implemented)", vsync);
    } else {
      throw new UnhandledIOPortException(machine, port);
    }
  }

  @Override
  public void outw(int port, int value) throws InvalidOperationException {
    if (port == VGA_SEQUENCER_ADDRESS_REGISTER_PORT || port == VGA_SEQUENCER_DATA_REGISTER_PORT
        || port == GRAPHICS_ADDRESS_REGISTER_PORT) {
      // Not implemented
      return;
    }
    throw new UnhandledIOPortException(machine, port);
  }
}
