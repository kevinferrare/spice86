package spice86.emulator.machine;

import spice86.emulator.callback.Callback;
import spice86.emulator.callback.CallbackHandler;
import spice86.emulator.cpu.Cpu;
import spice86.emulator.cpu.State;
import spice86.emulator.devices.externalinterrupt.Pic;
import spice86.emulator.devices.input.joystick.Joystick;
import spice86.emulator.devices.input.keyboard.Keyboard;
import spice86.emulator.devices.sound.GravisUltraSound;
import spice86.emulator.devices.sound.Midi;
import spice86.emulator.devices.sound.PcSpeaker;
import spice86.emulator.devices.sound.SoundBlaster;
import spice86.emulator.devices.timer.Timer;
import spice86.emulator.devices.video.VgaCard;
import spice86.emulator.errors.InvalidOperationException;
import spice86.emulator.function.CallType;
import spice86.emulator.function.FunctionHandler;
import spice86.emulator.interrupthandlers.bios.BiosEquipmentDeterminationInt11Handler;
import spice86.emulator.interrupthandlers.bios.SystemBiosInt15Handler;
import spice86.emulator.interrupthandlers.dos.DosInt20Handler;
import spice86.emulator.interrupthandlers.dos.DosInt21Handler;
import spice86.emulator.interrupthandlers.input.keyboard.BiosKeyboardInt9Handler;
import spice86.emulator.interrupthandlers.input.keyboard.KeyboardInt16Handler;
import spice86.emulator.interrupthandlers.input.mouse.MouseInt33Handler;
import spice86.emulator.interrupthandlers.systemclock.SystemClockInt1AHandler;
import spice86.emulator.interrupthandlers.timer.TimerInt8Handler;
import spice86.emulator.interrupthandlers.vga.VideoBiosInt10Handler;
import spice86.emulator.ioports.IOPortDispatcher;
import spice86.emulator.ioports.IOPortHandler;
import spice86.emulator.memory.Memory;
import spice86.emulator.memory.SegmentedAddress;
import spice86.ui.Gui;

/**
 * Emulates an IBM PC
 */
public class Machine {
  private static final int INTERRUPT_HANDLERS_SEGMENT = 0xF000;

  private Memory memory;
  private Cpu cpu;
  // IO Devices
  private IOPortDispatcher ioPortDispatcher;

  private Pic pic;
  private Timer timer;
  private SystemClockInt1AHandler systemClockInt1AHandler;
  private VgaCard vgaCard;
  private Keyboard keyboard;
  private Joystick joystick;
  private PcSpeaker pcSpeaker;
  private SoundBlaster soundBlaster;
  private GravisUltraSound gravisUltraSound;
  private Midi midi;

  private Gui gui;

  private CallbackHandler callbackHandler;
  // Services for int callbacks
  private TimerInt8Handler timerInt8Handler;
  private BiosKeyboardInt9Handler biosKeyboardInt9Handler;
  private VideoBiosInt10Handler videoBiosInt10Handler;
  private BiosEquipmentDeterminationInt11Handler biosEquipmentDeterminationInt11Handler;
  private SystemBiosInt15Handler systemBiosInt15Handler;
  private KeyboardInt16Handler keyboardInt16Handler;
  private DosInt20Handler dosInt20Handler;
  private DosInt21Handler dosInt21Handler;
  private MouseInt33Handler mouseInt33Handler;

  private MachineBreakpoints machineBreakpoints;

  public Machine(Gui gui, long instructionsPerSecond, boolean failOnUnhandledPort) {
    this.gui = gui;
    initHardware(instructionsPerSecond, failOnUnhandledPort);
    initServices();
  }

  public Memory getMemory() {
    return memory;
  }

  public Cpu getCpu() {
    return cpu;
  }

  public IOPortDispatcher getIoPortDispatcher() {
    return ioPortDispatcher;
  }

  public Pic getPic() {
    return pic;
  }

  public Timer getTimer() {
    return timer;
  }

  public SystemClockInt1AHandler getSystemClockInt1AHandler() {
    return systemClockInt1AHandler;
  }

  public VgaCard getVgaCard() {
    return vgaCard;
  }

  public Keyboard getKeyboard() {
    return keyboard;
  }

  public Joystick getJoystick() {
    return joystick;
  }

  public PcSpeaker getPcSpeaker() {
    return pcSpeaker;
  }

  public SoundBlaster getSoundBlaster() {
    return soundBlaster;
  }

  public GravisUltraSound getGravisUltraSound() {
    return gravisUltraSound;
  }

  public Midi getMidi() {
    return midi;
  }

  public Gui getGui() {
    return gui;
  }

  public CallbackHandler getCallbackHandler() {
    return callbackHandler;
  }

  public TimerInt8Handler getTimerInt8Handler() {
    return timerInt8Handler;
  }

  public BiosKeyboardInt9Handler getBiosKeyboardInt9Handler() {
    return biosKeyboardInt9Handler;
  }

  public VideoBiosInt10Handler getVideoBiosInt10Handler() {
    return videoBiosInt10Handler;
  }

  public BiosEquipmentDeterminationInt11Handler getBiosEquipmentDeterminationInt11Handler() {
    return biosEquipmentDeterminationInt11Handler;
  }

  public SystemBiosInt15Handler getSystemBiosInt15Handler() {
    return systemBiosInt15Handler;
  }

  public KeyboardInt16Handler getKeyboardInt16Handler() {
    return keyboardInt16Handler;
  }

  public DosInt20Handler getDosInt20Handler() {
    return dosInt20Handler;
  }

  public DosInt21Handler getDosInt21Handler() {
    return dosInt21Handler;
  }

  public MouseInt33Handler getMouseInt33Handler() {
    return mouseInt33Handler;
  }

  public MachineBreakpoints getMachineBreakpoints() {
    return machineBreakpoints;
  }

  public String peekReturn() {
    return toString(cpu.getFunctionHandlerInUse().peekReturnAddressOnMachineStackForCurrentFunction());
  }

  public String peekReturn(CallType returnCallType) {
    return toString(cpu.getFunctionHandlerInUse().peekReturnAddressOnMachineStack(returnCallType));
  }

  private String toString(SegmentedAddress segmentedAddress) {
    if (segmentedAddress != null) {
      return segmentedAddress.toString();
    }
    return "null";
  }

  private final void initHardware(long instructionsPerSecond, boolean failOnUnhandledPort) {
    // A full 1MB of addressable memory :)
    memory = new Memory(0x100_000);

    cpu = new Cpu(this);
    cpu.getFunctionHandler().setMachine(this);
    cpu.getFunctionHandlerInExternalInterrupt().setMachine(this);

    // Breakpoints
    machineBreakpoints = new MachineBreakpoints(this);

    // IO devices
    ioPortDispatcher = new IOPortDispatcher(this, failOnUnhandledPort);
    cpu.setIoPortDispatcher(ioPortDispatcher);

    pic = new Pic(this, true, failOnUnhandledPort);
    register(pic);

    vgaCard = new VgaCard(this, gui, failOnUnhandledPort);
    register(vgaCard);

    timer = new Timer(this, pic, vgaCard, instructionsPerSecond, failOnUnhandledPort);
    register(timer);

    keyboard = new Keyboard(this, gui, failOnUnhandledPort);
    register(keyboard);

    joystick = new Joystick(this, failOnUnhandledPort);
    register(joystick);

    pcSpeaker = new PcSpeaker(this, failOnUnhandledPort);
    register(pcSpeaker);

    soundBlaster = new SoundBlaster(this, failOnUnhandledPort);
    register(soundBlaster);

    gravisUltraSound = new GravisUltraSound(this, failOnUnhandledPort);
    register(gravisUltraSound);

    midi = new Midi(this, failOnUnhandledPort);
    register(midi);
  }

  private void initServices() {
    callbackHandler = new CallbackHandler(this, INTERRUPT_HANDLERS_SEGMENT);
    cpu.setCallbackHandler(callbackHandler);

    timerInt8Handler = new TimerInt8Handler(this);
    register(timerInt8Handler);

    biosKeyboardInt9Handler = new BiosKeyboardInt9Handler(this);
    register(biosKeyboardInt9Handler);

    videoBiosInt10Handler = new VideoBiosInt10Handler(this, vgaCard);
    videoBiosInt10Handler.initRam();
    register(videoBiosInt10Handler);

    biosEquipmentDeterminationInt11Handler = new BiosEquipmentDeterminationInt11Handler(this);
    register(biosEquipmentDeterminationInt11Handler);

    systemBiosInt15Handler = new SystemBiosInt15Handler(this);
    register(systemBiosInt15Handler);

    keyboardInt16Handler =
        new KeyboardInt16Handler(this, biosKeyboardInt9Handler.getBiosKeyboardBuffer());
    register(keyboardInt16Handler);

    systemClockInt1AHandler = new SystemClockInt1AHandler(this, timerInt8Handler);
    register(systemClockInt1AHandler);

    dosInt20Handler = new DosInt20Handler(this);
    register(dosInt20Handler);

    dosInt21Handler = new DosInt21Handler(this);
    register(dosInt21Handler);

    mouseInt33Handler = new MouseInt33Handler(this, gui);
    register(mouseInt33Handler);
  }

  public void register(IOPortHandler ioPortHandler) {
    ioPortHandler.initPortHandlers(ioPortDispatcher);
  }

  public void register(Callback callback) {
    callbackHandler.addCallback(callback);
  }

  public void installAllCallbacksInInterruptTable() {
    callbackHandler.installAllCallbacksInInterruptTable();
  }

  public void run() throws InvalidOperationException {
    State state = cpu.getState();
    FunctionHandler functionHandler = cpu.getFunctionHandler();
    functionHandler.call(CallType.MACHINE, state.getCS(), state.getIP(), null, null, () -> "entry", false);
    runLoop();
    machineBreakpoints.onMachineStop();
    functionHandler.ret(CallType.MACHINE);
  }

  private void runLoop() throws InvalidOperationException {
    while (cpu.isRunning()) {
      machineBreakpoints.checkBreakPoint();
      cpu.executeNextInstruction();
      timer.tick();
    }
  }

  public String dumpCallStack() {
    FunctionHandler inUse = cpu.getFunctionHandlerInUse();
    String callStack = "";
    if (inUse.equals(cpu.getFunctionHandlerInExternalInterrupt())) {
      callStack += "From external interrupt:\n";
    }
    callStack += inUse.dumpCallStack();
    return callStack;
  }
}
