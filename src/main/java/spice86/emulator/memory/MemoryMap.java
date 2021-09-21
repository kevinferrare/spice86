package spice86.emulator.memory;

/**
 * Informations about memory mapping of an IBM PC
 */
public class MemoryMap {
  public static final int INTERRUPT_VECTOR_SEGMENT = 0x0;
  public static final int INTERRUPT_VECTOR_LENGTH = 1024;

  public static final int BIOS_DATA_AREA_SEGMENT = 0x40;
  public static final int BIOS_DATA_AREA_LENGTH = 256;

  public static final int FREE_MEMORY_START_SEGMENT = 0x50;
  // This is where the port to get VGA CRT status is stored
  public static final int BIOS_DATA_AREA_OFFSET_CRT_IO_PORT = 0x63;
  // Counter incremented 18.2 times per second
  public static final int BIOS_DATA_AREA_OFFSET_TICK_COUNTER = 0x6C;

  public static final int BOOT_SECTOR_CODE_SEGMENT = 0x07C0;
  public static final int BOOT_SECTOR_CODE_LENGTH = 512;

  public static final int GRAPHIC_VIDEO_MEMORY_SEGMENT = 0xA000;
  public static final int GRAPHIC_VIDEO_MEMORY_LENGTH = 65535;

  public static final int MONOCHROME_TEXT_VIDEO_MEMORY_SEGMENT = 0xB000;
  public static final int MONOCHROME_TEXT_VIDEO_MEMORY_LENGTH = 32767;

  public static final int COLOR_TEXT_VIDEO_MEMORY_SEGMENT = 0xB800;
  public static final int COLOR_TEXT_VIDEO_MEMORY_LENGTH = 32767;
}
