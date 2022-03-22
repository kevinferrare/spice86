package spice86.emulator.interrupthandlers.input.mouse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.errors.UnhandledOperationException;
import spice86.emulator.interrupthandlers.InterruptHandler;
import spice86.emulator.machine.Machine;
import spice86.ui.Gui;
import spice86.utils.ConvertUtils;

/**
 * Interface between the mouse and the emulator.<br/>
 * Re-implements int33.<br/>
 */
public class MouseInt33Handler extends InterruptHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(MouseInt33Handler.class);
  private static final int MOUSE_RANGE_X = 639;
  private static final int MOUSE_RANGE_Y = 199;
  private Gui gui;

  private int mouseMinX;
  private int mouseMinY;
  private int mouseMaxX = MOUSE_RANGE_X;
  private int mouseMaxY = MOUSE_RANGE_Y;
  private int userCallbackMask;
  private int userCallbackSegment;
  private int userCallbackOffset;

  public MouseInt33Handler(Machine machine, Gui gui) {
    super(machine);
    this.gui = gui;
    super.dispatchTable.put(0x00, this::mouseInstalledFlag);
    super.dispatchTable.put(0x03, this::getMousePositionAndStatus);
    super.dispatchTable.put(0x04, this::setMouseCursorPosition);
    super.dispatchTable.put(0x07, this::setMouseHorizontalMinMaxPosition);
    super.dispatchTable.put(0x08, this::setMouseVerticalMinMaxPosition);
    super.dispatchTable.put(0x0C, this::setMouseUserDefinedSubroutine);
    super.dispatchTable.put(0x0F, this::setMouseMickeyPixelRatio);
    super.dispatchTable.put(0x13, this::setMouseDoubleSpeedThreshold);
    super.dispatchTable.put(0x14, this::swapMouseUserDefinedSubroutine);
    super.dispatchTable.put(0x1A, this::setMouseSensivity);

  }

  /**
   *
   * @param value Raw value from the GUI
   * @param maxValue Max that value can be
   * @param min mix expected by program
   * @param max max expected by program
   * @return
   */
  private int restrictValue(int value, int maxValue, int min, int max) {
    int range = max - min;
    int valueInRange = (value * range / maxValue);
    if (valueInRange > max) {
      return max;
    }
    if (valueInRange < min) {
      return min;
    }
    return valueInRange;
  }

  public void mouseInstalledFlag() {
    LOGGER.info("MOUSE INSTALLED FLAG");
    state.setAX(0xFFFF);
    // 3 buttons
    state.setBX(3);
  }

  public void getMousePositionAndStatus() {
    int x = restrictValue(gui.getMouseX(), gui.getWidth(), mouseMinX, mouseMaxX);
    int y = restrictValue(gui.getMouseY(), gui.getHeight(), mouseMinY, mouseMaxY);
    boolean leftClick = gui.isLeftButtonClicked();
    boolean rightClick = gui.isRightButtonClicked();
    LOGGER.info("GET MOUSE POSITION AND STATUS x={}, y={}, leftClick={}, rightClick={}", x, y, leftClick, rightClick);
    state.setCX(x);
    state.setDX(y);
    state.setBX((leftClick ? 1 : 0) | ((rightClick ? 1 : 0) << 1));
  }

  private int oldX = -1;
  private int oldY = -1;

  public void setMouseCursorPosition() {
    int x = state.getCX();
    int y = state.getDX();
    LOGGER.info("SET MOUSE CURSOR POSITION x={}, y={}", x, y);
    if(oldX == gui.getMouseX() && oldY == gui.getMouseY()) {
      return;
    }
    oldX = gui.getMouseX();
    oldY = gui.getMouseY();
    gui.setMouseX(x);
    gui.setMouseY(y);
  }

  public void setMouseHorizontalMinMaxPosition() {
    this.mouseMinX = state.getCX();
    this.mouseMaxX = state.getDX();
    LOGGER.info("SET MOUSE HORIZONTAL MIN MAX POSITION minX={}, maxX={}", mouseMinX, mouseMaxX);
  }

  public void setMouseVerticalMinMaxPosition() {
    this.mouseMinY = state.getCX();
    this.mouseMaxY = state.getDX();
    LOGGER.info("SET MOUSE VERTICAL MIN MAX POSITION minY={}, maxY={}", mouseMinY, mouseMaxY);
  }

  public void setMouseUserDefinedSubroutine() {
    userCallbackMask = state.getCX();
    userCallbackSegment = state.getES();
    userCallbackOffset = state.getDX();
    LOGGER.info("SET MOUSE USER DEFINED SUBROUTINE (unimplemented!) mask={}, segment={}, offset={}", userCallbackMask,
        userCallbackSegment, userCallbackOffset);
  }

  public void setMouseMickeyPixelRatio() {
    int rx = state.getCX();
    int ry = state.getDX();
    LOGGER.info("SET MOUSE MICKEY PIXEL RATIO rx={}, ry={}", rx, ry);
  }

  public void setMouseDoubleSpeedThreshold() {
    int threshold = state.getDX();
    LOGGER.info("SET MOUSE DOUBLE SPEED THRESHOLD threshold={}", threshold);
  }

  public void swapMouseUserDefinedSubroutine() {
    int newUserCallbackMask = state.getCX();
    int newUserCallbackSegment = state.getES();
    int newUserCallbackOffset = state.getDX();
    LOGGER.info("SWAP MOUSE USER DEFINED SUBROUTINE (unimplemented!) mask={}, segment={}, offset={}",
        newUserCallbackMask, newUserCallbackSegment, newUserCallbackOffset);
    state.setCX(userCallbackMask);
    state.setES(userCallbackSegment);
    state.setDX(userCallbackOffset);
    userCallbackMask = newUserCallbackMask;
    userCallbackOffset = newUserCallbackOffset;
    userCallbackSegment = newUserCallbackSegment;
  }

  public void setMouseSensivity() {
    int horizontalSpeed = state.getBX();
    int verticalSpeed = state.getCX();
    int threshold = state.getDX();
    LOGGER.info("SET MOUSE SENSIVITY horizontalSpeed={}, verticalSpeed={}, threshold={}", horizontalSpeed,
        verticalSpeed, threshold);
  }

  @Override
  public void run() throws UnhandledOperationException {
    int operation = ConvertUtils.uint8(state.getAX());
    this.run(operation);
  }

  @Override
  public int getIndex() {
    return 0x33;
  }

}
