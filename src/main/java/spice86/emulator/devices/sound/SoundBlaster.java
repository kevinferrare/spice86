package spice86.emulator.devices.sound;

import spice86.emulator.ioports.DummyIOPortHandler;
import spice86.emulator.ioports.IOPortDispatcher;

/**
 * Sound blaster implementation. Emulates an absent card :) http://www.fysnet.net/detectsb.htm
 */
public class SoundBlaster extends DummyIOPortHandler {
  private static final int LEFT_SPEAKER_STATUS_PORT_NUMBER = 0x220;
  private static final int LEFT_SPEAKER_DATA_PORT_NUMBER = 0x221;
  private static final int RIGHT_SPEAKER_STATUS_PORT_NUMBER = 0x222;
  private static final int RIGHT_SPEAKER_DATA_PORT_NUMBER = 0x223;
  private static final int MIXER_REGISTER_PORT_NUMBER = 0x224;
  private static final int MIXER_DATA_PORT_NUMBER = 0x225;
  private static final int DSP_RESET_PORT_NUMBER = 0x226;
  private static final int FM_MUSIC_STATUS_PORT_NUMBER = 0x228;
  private static final int FM_MUSIC_STATUS_PORT_NUMBER_2 = 0x388;
  private static final int FM_MUSIC_DATA_PORT_NUMBER = 0x229;
  private static final int FM_MUSIC_DATA_PORT_NUMBER_2 = 0x389;
  private static final int DSP_READ_PORT_NUMBER = 0x22A;
  private static final int DSP_WRITE_BUFFER_STATUS_PORT_NUMBER = 0x22C;
  private static final int DSP_DATA_AVAILABLE_PORT_NUMBER = 0x22E;

  @Override
  public void initPortHandlers(IOPortDispatcher ioPortDispatcher) {
    ioPortDispatcher.addIOPortHandler(LEFT_SPEAKER_STATUS_PORT_NUMBER, this);
    ioPortDispatcher.addIOPortHandler(LEFT_SPEAKER_DATA_PORT_NUMBER, this);
    ioPortDispatcher.addIOPortHandler(RIGHT_SPEAKER_STATUS_PORT_NUMBER, this);
    ioPortDispatcher.addIOPortHandler(RIGHT_SPEAKER_DATA_PORT_NUMBER, this);
    ioPortDispatcher.addIOPortHandler(MIXER_REGISTER_PORT_NUMBER, this);
    ioPortDispatcher.addIOPortHandler(MIXER_DATA_PORT_NUMBER, this);
    ioPortDispatcher.addIOPortHandler(DSP_RESET_PORT_NUMBER, this);
    ioPortDispatcher.addIOPortHandler(FM_MUSIC_STATUS_PORT_NUMBER, this);
    ioPortDispatcher.addIOPortHandler(FM_MUSIC_STATUS_PORT_NUMBER_2, this);
    ioPortDispatcher.addIOPortHandler(FM_MUSIC_DATA_PORT_NUMBER, this);
    ioPortDispatcher.addIOPortHandler(FM_MUSIC_DATA_PORT_NUMBER_2, this);
    ioPortDispatcher.addIOPortHandler(DSP_READ_PORT_NUMBER, this);
    ioPortDispatcher.addIOPortHandler(DSP_WRITE_BUFFER_STATUS_PORT_NUMBER, this);
    ioPortDispatcher.addIOPortHandler(DSP_DATA_AVAILABLE_PORT_NUMBER, this);
  }
}
