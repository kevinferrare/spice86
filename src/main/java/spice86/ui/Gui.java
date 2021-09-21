package spice86.ui;

import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import spice86.emulator.devices.video.Rgb;
import spice86.utils.ConvertUtils;

/**
 * GUI of the emulator.<br/>
 * <ul>
 * <li>Displays the content of the video ram (when the emulator requests it)</li>
 * <li>Communicates keyboard and mouse events to the emulator</li>
 * </ul>
 */
public class Gui {
  private static final Logger LOGGER = LoggerFactory.getLogger(Gui.class);

  private Stage stage;
  private Group group = new Group();
  private Canvas canvas;
  private int width = 1;
  private int height = 1;
  private KeyCode lastKeyCode = null;
  private Set<KeyCode> keysPressed = new HashSet<>();
  private int mouseX;
  private int mouseY;
  private boolean leftButtonClicked;
  private boolean rightButtonClicked;

  private Runnable onKeyPressedEvent;
  private Runnable onKeyReleasedEvent;

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public KeyCode getLastKeyCode() {
    return lastKeyCode;
  }

  public boolean isKeyPressed(KeyCode keyCode) {
    return keysPressed.contains(keyCode);
  }

  public int getMouseX() {
    return mouseX;
  }

  public int getMouseY() {
    return mouseY;
  }

  public void setMouseX(int mouseX) {
    this.mouseX = mouseX;
  }

  public void setMouseY(int mouseY) {
    this.mouseY = mouseY;
  }

  public boolean isLeftButtonClicked() {
    return leftButtonClicked;
  }

  public boolean isRightButtonClicked() {
    return rightButtonClicked;
  }

  private void onKeyPressed(KeyEvent event) {
    KeyCode keyCode = event.getCode();
    if (!keysPressed.contains(keyCode)) {
      LOGGER.info("Key pressed {}", keyCode);
      keysPressed.add(keyCode);
      this.lastKeyCode = keyCode;
      runOnKeyEvent(this.onKeyPressedEvent);
    }
  }

  private void onKeyReleased(KeyEvent event) {
    this.lastKeyCode = event.getCode();
    LOGGER.info("Key released {}", lastKeyCode);
    keysPressed.remove(lastKeyCode);
    runOnKeyEvent(this.onKeyReleasedEvent);
  }

  private void runOnKeyEvent(Runnable runnable) {
    if (runnable != null) {
      runnable.run();
    }
  }

  public void setOnKeyPressedEvent(Runnable onKeyPressedEvent) {
    this.onKeyPressedEvent = onKeyPressedEvent;
  }

  public void setOnKeyReleasedEvent(Runnable onKeyReleasedEvent) {
    this.onKeyReleasedEvent = onKeyReleasedEvent;
  }

  private void onMouseMoved(MouseEvent event) {
    int mouseXInCanvas = (int)event.getX();
    int mouseYInCanvas = (int)event.getY();
    // Mouse coords are in this range
    mouseX = mouseXInCanvas * 639 / width;
    mouseY = mouseYInCanvas * 199 / height;
  }

  private void onMouseClick(MouseEvent event, boolean click) {
    if (event.getButton() == MouseButton.PRIMARY) {
      this.leftButtonClicked = click;
    }
    if (event.getButton() == MouseButton.SECONDARY) {
      rightButtonClicked = click;
    }

  }

  public void setResolution(int width, int height) {
    this.width = width;
    this.height = height;
    canvas = new Canvas(width, height);
    canvas.getGraphicsContext2D();
    Scale scale = new Scale();
    scale.setPivotX(0);
    scale.setPivotY(0);
    scale.setX(4);
    scale.setY(4);
    canvas.getTransforms().add(scale);
    canvas.setOnMouseMoved(this::onMouseMoved);
    canvas.setOnMousePressed(event -> onMouseClick(event, true));
    canvas.setOnMouseReleased(event -> onMouseClick(event, false));
    if (stage != null) {
      Platform.runLater(() -> {
        group.getChildren().clear();
        group.getChildren().add(canvas);
        stage.sizeToScene();
      });
    }
  }

  public void draw(byte[] memory, int startAddress, Rgb[] palette) {
    if (canvas == null) {
      return;
    }
    int size = width * height;
    IntBuffer buffer = IntBuffer.allocate(size);
    int endAddress = startAddress + size;
    for (int i = startAddress; i < endAddress; i++) {
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

  public void setStage(Stage stage) {
    this.stage = stage;
    Scene scene = new Scene(group);
    scene.setCursor(Cursor.NONE);
    scene.setOnKeyPressed(this::onKeyPressed);
    scene.setOnKeyReleased(this::onKeyReleased);
    this.stage.setScene(scene);
  }
}
