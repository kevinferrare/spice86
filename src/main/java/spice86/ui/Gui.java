package spice86.ui;

import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spice86.emulator.devices.video.Rgb;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

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
  private int mainCanvasScale = 3;
  private AnchorPane layout = new AnchorPane();
  // Map associating a start address to a canvas
  private Map<Integer, VideoBuffer> videoBuffers = new HashMap<>();
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
    setMouseX((int)event.getX());
    setMouseY((int)event.getY());
  }

  private void onMouseClick(MouseEvent event, boolean click) {
    if (event.getButton() == MouseButton.PRIMARY) {
      this.leftButtonClicked = click;
    }
    if (event.getButton() == MouseButton.SECONDARY) {
      rightButtonClicked = click;
    }

  }

  public void setResolution(int width, int height, int address) {
    videoBuffers.clear();
    this.width = width;
    this.height = height;
    addBuffer(address, mainCanvasScale, width, height, canvas -> {
      canvas.setOnMouseMoved(this::onMouseMoved);
      canvas.setOnMousePressed(event -> onMouseClick(event, true));
      canvas.setOnMouseReleased(event -> onMouseClick(event, false));
    });
  }

  public void addBuffer(int address, double scale, int bufferWidth, int bufferHeight, Consumer<Canvas> canvasPostSetupAction) {
    VideoBuffer videoBuffer = new VideoBuffer(bufferWidth, bufferHeight, scale, address, videoBuffers.size());
    Canvas canvas = videoBuffer.getCanvas();
    videoBuffers.put(address, videoBuffer);
    if (canvasPostSetupAction != null) {
      canvasPostSetupAction.accept(canvas);
    }
    relayout();
  }

  private void relayout() {
    if (stage != null) {
      Platform.runLater(() -> {
        layout.getChildren().clear();
        for (VideoBuffer videoBuffer : sortedBuffers()) {
          layoutOneBuffer(videoBuffer);
        }
        stage.sizeToScene();
      });
    }
  }

  private void layoutOneBuffer(VideoBuffer videoBuffer) {
    Group group = new Group();
    group.getChildren().add(videoBuffer.getCanvas());

    if (videoBuffer.getIndex() == 0) {
      // Main display
      AnchorPane.setTopAnchor(group, 0.0);
      AnchorPane.setLeftAnchor(group, 0.0);
      layout.getChildren().clear();
      layout.getChildren().add(group);
    } else {
      // Additional buffers, below
      int additionalCanvasIndex = videoBuffer.getIndex() - 1;
      int line = additionalCanvasIndex % mainCanvasScale;
      int column = additionalCanvasIndex / mainCanvasScale;
      AnchorPane.setTopAnchor(group, (double)height * line);
      AnchorPane.setLeftAnchor(group, (double)width * (mainCanvasScale + column));
      layout.getChildren().add(group);
    }
  }

  private Set<VideoBuffer> sortedBuffers() {
    return new TreeSet<>(videoBuffers.values());
  }

  public void removeBuffer(int address) {
    videoBuffers.remove(address);
    relayout();
  }

  public void draw(byte[] memory, Rgb[] palette) {
    for (VideoBuffer videoBuffer : sortedBuffers()) {
      videoBuffer.draw(memory, palette);
    }
  }

  public Map<Integer, VideoBuffer> getVideoBuffers() {
    return videoBuffers;
  }

  public void setStage(Stage stage) {
    this.stage = stage;
    Scene scene = new Scene(layout);
    scene.setCursor(Cursor.NONE);
    scene.setOnKeyPressed(this::onKeyPressed);
    scene.setOnKeyReleased(this::onKeyReleased);
    this.stage.setScene(scene);
  }
}
