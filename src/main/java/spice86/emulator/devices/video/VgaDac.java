package spice86.emulator.devices.video;

import static spice86.utils.ConvertUtils.uint8;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import spice86.emulator.machine.Machine;

/**
 * VGA Digital Analog Converter Implementation.
 */
public class VgaDac {
  private static final int RED_INDEX = 0;
  private static final int BLUE_INDEX = 2;
  private static final int GREEN_INDEX = 1;
  public static final int VGA_DAC_NOT_INITIALIZED = 0;
  public static final int VGA_DAC_READ = 1;
  public static final int VGA_DAC_WRITE = 2;

  public static int from8bitTo6bitColor(int color8bit) {
    return uint8(color8bit >>> 2);
  }

  public static int from6bitColorTo8bit(int color6bit) {
    return uint8((color6bit & 0b111111) << 2);
  }

  private Machine machine;
  private int state = 1;
  private int colour; /* 0 = red, 1 = green, 2 = blue */
  private int readIndex;
  private int writeIndex;

  private Rgb[] rgbs = new Rgb[256];

  public VgaDac(Machine machine) {
    this.machine = machine;
    // Initial VGA default palette initialization
    for (int i = 0; i < rgbs.length; i++) {
      Rgb rgb = new Rgb();
      rgb.setR((((i >>> 5) & 0x7) * 255 / 7));
      rgb.setG((((i >>> 2) & 0x7) * 255 / 7));
      rgb.setB(((i & 0x3) * 255 / 3));
      rgbs[i] = rgb;
    }
  }

  public void writeColor(int colorValue) throws InvalidColorIndexException {
    Rgb rgb = rgbs[writeIndex];
    if (colour == RED_INDEX) {
      rgb.setR(colorValue);
    } else if (colour == GREEN_INDEX) {
      rgb.setG(colorValue);
    } else if (colour == BLUE_INDEX) {
      rgb.setB(colorValue);
    } else {
      throw new InvalidColorIndexException(machine, colour);
    }
    colour = (colour + 1) % 3;
    if (colour == 0) {
      writeIndex++;
    }
  }

  public int readColor() throws InvalidColorIndexException {
    Rgb rgb = rgbs[readIndex];
    int value = switch (colour) {
      case RED_INDEX -> rgb.getR();
      case GREEN_INDEX -> rgb.getR();
      case BLUE_INDEX -> rgb.getR();
      default -> throw new InvalidColorIndexException(machine, colour);
    };
    colour = (colour + 1) % 3;
    if (colour == 0) {
      writeIndex++;
    }
    return value;
  }

  public int getState() {
    return state;
  }

  public void setState(int state) {
    this.state = state;
  }

  public int getColour() {
    return colour;
  }

  public void setColour(int colour) {
    this.colour = colour;
  }

  public int getReadIndex() {
    return readIndex;
  }

  public void setReadIndex(int readIndex) {
    this.readIndex = readIndex;
  }

  public int getWriteIndex() {
    return writeIndex;
  }

  public void setWriteIndex(int writeIndex) {
    this.writeIndex = writeIndex;
  }

  public Rgb[] getRgbs() {
    return rgbs;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.JSON_STYLE).toString();
  }
}
