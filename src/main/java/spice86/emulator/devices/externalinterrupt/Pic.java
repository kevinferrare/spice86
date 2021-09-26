package spice86.emulator.devices.externalinterrupt;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.errors.InvalidOperationException;
import spice86.emulator.errors.UnhandledOperationException;
import spice86.emulator.ioports.DefaultIOPortHandler;
import spice86.emulator.ioports.IOPortDispatcher;
import spice86.emulator.machine.Machine;
import spice86.utils.ConvertUtils;

/**
 * Emulates a PIC8259 Programmable Interrupt Controller.<br/>
 * Some resources:
 * <ul>
 * <li>https://wiki.osdev.org/PIC</li>
 * <li>https://k.lse.epita.fr/internals/8259a_controller.html</li>
 * </ul>
 */
public class Pic extends DefaultIOPortHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(Pic.class);
  private static final int MASTER_PORT_A = 0x20;
  private static final int MASTER_PORT_B = 0x21;
  private static final int SLAVE_PORT_A = 0xA0;
  private static final int SLAVE_PORT_B = 0xA1;
  private static final Map<Integer, Integer> VECTOR_NUMBER_TO_IRQ = new HashMap<>();
  static {
    // timer
    VECTOR_NUMBER_TO_IRQ.put(8, 0);
    // keyboard
    VECTOR_NUMBER_TO_IRQ.put(9, 1);
  }

  private boolean inintialized = false;
  private int currentCommand = 0;
  private int commandsToProcess = 2;

  private int interruptMask = 0;
  private boolean lastIrqAcknowledged = true;

  public Pic(Machine machine, boolean initialized, boolean failOnUnhandledPort) {
    super(machine, failOnUnhandledPort);
    this.inintialized = initialized;
  }

  public void acknwowledgeInterrupt() {
    lastIrqAcknowledged = true;
  }

  public void processInterrupt(int vectorNumber) {
    if (irqMasked(vectorNumber)) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Cannot process interrupt {}, IRQ is masked.", ConvertUtils.toHex8(vectorNumber));
      }
      return;
    }
    if (!isLastIrqAcknowledged()) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Cannot process interrupt {}, Last IRQ was not acknowledged.",
            ConvertUtils.toHex8(vectorNumber));
      }
      return;
    }

    lastIrqAcknowledged = false;
    cpu.externalInterrupt(vectorNumber);
  }

  public boolean isLastIrqAcknowledged() {
    return lastIrqAcknowledged;
  }

  public boolean irqMasked(int vectorNumber) {
    Integer irqNumber = VECTOR_NUMBER_TO_IRQ.get(vectorNumber);
    if (irqNumber == null) {
      return false;
    }
    int maskForVectorNumber = (1 << irqNumber);
    return (maskForVectorNumber & interruptMask) != 0;
  }

  private void processPortACommand(int value) throws UnhandledOperationException {
    if (!inintialized) {
      // Process initialization commands
      switch (currentCommand) {
        case 1 -> processICW2(value);
        case 2 -> processICW3(value);
        case 3 -> processICW4(value);
        default -> throw new UnhandledOperationException(machine,
            "Invalid initialization command index " + currentCommand + ", should never happen");
      }
      currentCommand = (currentCommand + 1) % commandsToProcess;
      if (currentCommand == 0) {
        commandsToProcess = 2;
        inintialized = true;
      }
    } else {
      processOCW2(value);
    }
  }

  private void processPortBCommand(int value) {
    if (!inintialized) {
      processICW1(value);
      currentCommand = 1;
    } else {
      processOCW1(value);
    }
  }

  private void processICW1(int value) {
    boolean icw4Present = (value & 0b1) == 1;
    boolean singleController = (value & 0b10) == 1;
    boolean levelTriggered = (value & 0b1000) == 1;
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("MASTER PIC COMMAND ICW1 {}. icw4Present={}, singleController={}, levelTriggered={}",
          ConvertUtils.toHex8(value), icw4Present, singleController, levelTriggered);
    }
    commandsToProcess = icw4Present ? 4 : 3;
  }

  private void processICW2(int value) {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("MASTER PIC COMMAND ICW2 {}. baseOffsetInInterruptDescriptorTable={}", ConvertUtils.toHex8(value),
          value);
    }
  }

  private void processICW3(int value) {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("PIC COMMAND ICW3 {}.", ConvertUtils.toHex8(value));
    }
  }

  private void processICW4(int value) {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("PIC COMMAND ICW4 {}.", ConvertUtils.toHex8(value));
    }
  }

  private void processOCW1(int value) {
    interruptMask = value;
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("PIC COMMAND OCW1 {}. Mask is {}", ConvertUtils.toHex8(value), ConvertUtils.toBin8(value));
    }
  }

  private void processOCW2(int value) {
    int interruptLevel = value & 0b111;
    boolean sendEndOfInterruptCommand = (value & 0b100000) != 0;
    lastIrqAcknowledged = sendEndOfInterruptCommand;
    boolean sendSpecificCommand = (value & 0b1000000) != 0;
    boolean rotatePriorities = (value & 0b10000000) != 0;
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info(
          "PIC COMMAND OCW2 {}. interruptLevel={}, sendEndOfInterruptCommand={}, sendSpecificCommand={}, rotatePriorities{}",
          ConvertUtils.toHex8(value), interruptLevel, sendEndOfInterruptCommand, sendSpecificCommand, rotatePriorities);
    }
  }

  @Override
  public void initPortHandlers(IOPortDispatcher ioPortDispatcher) {
    ioPortDispatcher.addIOPortHandler(MASTER_PORT_A, this);
    ioPortDispatcher.addIOPortHandler(MASTER_PORT_B, this);
    ioPortDispatcher.addIOPortHandler(SLAVE_PORT_A, this);
    ioPortDispatcher.addIOPortHandler(SLAVE_PORT_B, this);
  }

  @Override
  public void outb(int port, int value) throws InvalidOperationException {
    if (port == MASTER_PORT_A) {
      processPortACommand(value);
      return;
    } else if (port == MASTER_PORT_B) {
      processPortBCommand(value);
      return;
    }
    super.outb(port, value);
  }
}
