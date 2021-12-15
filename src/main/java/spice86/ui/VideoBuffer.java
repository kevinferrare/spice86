package spice86.ui;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.transform.Scale;
import spice86.emulator.devices.video.Rgb;
import spice86.utils.ConvertUtils;

import java.io.Serializable;
import java.nio.IntBuffer;

public class VideoBuffer implements Comparable<VideoBuffer>, Serializable {
  private int address;
  private int width;
  private int height;
  private double scaleFactor;
  private int index;
  private transient Canvas canvas;

  public VideoBuffer(int width, int height, double scaleFactor, int address, int index) {
    this.width = width;
    this.height = height;
    this.scaleFactor = scaleFactor;
    this.address = address;
    this.index = index;
    this.canvas = new Canvas(width, height);
    canvas.getGraphicsContext2D();
    if (scaleFactor != 1) {
      Scale scale = new Scale();
      scale.setPivotX(0);
      scale.setPivotY(0);
      scale.setX(this.scaleFactor);
      scale.setY(this.scaleFactor);
      canvas.getTransforms().add(scale);
    }
  }

  public Canvas getCanvas() {
    return canvas;
  }

  public int getIndex() {
    return index;
  }

  public void draw(byte[] memory, Rgb[] palette) {
    if (canvas == null) {
      return;
    }
    int size = width * height;
    IntBuffer buffer = IntBuffer.allocate(size);
    int endAddress = address + size;
    for (int i = address; i < endAddress; i++) {
      int colorIndex = ConvertUtils.uint8(memory[i]);
      Rgb pixel = palette[colorIndex];
      int argb = pixel.toArgb();
      buffer.put(argb);
    }
    buffer.flip();
    Platform.runLater(() -> {
      GraphicsContext gc = canvas.getGraphicsContext2D();
      PixelWriter pw = gc.getPixelWriter();
      pw.setPixels(0, 0, width, height,
          PixelFormat.getIntArgbInstance(), buffer, width);
    });
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }

  @Override
  public int compareTo(VideoBuffer o) {
    return Integer.compare(this.index, o.index);
  }

  @Override
  public boolean equals(Object o) {
    return this == o || (o instanceof VideoBuffer that) && this.index == that.index;
  }

  @Override
  public int hashCode() {
    return index;
  }
}
