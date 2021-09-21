package spice86.emulator.gdb;

import static spice86.utils.ConvertUtils.bytesToInt32;
import static spice86.utils.ConvertUtils.hexToByteArray;
import static spice86.utils.ConvertUtils.parseHex32;
import static spice86.utils.ConvertUtils.swap32;
import static spice86.utils.ConvertUtils.uint32i;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.cpu.State;
import spice86.emulator.machine.Machine;

public class GdbCommandRegisterHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(GdbCommandRegisterHandler.class);
  private GdbIo gdbIo;
  private Machine machine;
  private GdbFormatter gdbFormatter = new GdbFormatter();

  public GdbCommandRegisterHandler(GdbIo gdbIo, Machine machine) {
    this.gdbIo = gdbIo;
    this.machine = machine;
  }

  public String writeRegister(String commandContent) {
    String[] split = commandContent.split("=");
    int registerIndex = uint32i(parseHex32(split[0]));
    int registerValue = swap32(uint32i(parseHex32(split[1])));
    setRegisterValue(registerIndex, registerValue);
    return gdbIo.generateResponse("OK");
  }

  public String readRegister(String commandContent) {
    try {
      long index = parseHex32(commandContent);
      LOGGER.info("Reading register {}", index);
      return gdbIo.generateResponse(gdbFormatter.formatValueAsHex32(getRegisterValue(uint32i(index))));
    } catch (NumberFormatException nfe) {
      LOGGER.error("Register read requested but could not understand the request {}", commandContent);
      return gdbIo.generateUnsupportedResponse();
    }
  }

  public String writeAllRegisters(String commandContent) {
    try {
      byte[] data = hexToByteArray(commandContent);
      for (int i = 0; i < data.length; i += 4) {
        long value = bytesToInt32(data, i);
        setRegisterValue(i / 4, uint32i(value));
      }
      return gdbIo.generateResponse("OK");
    } catch (NumberFormatException nfe) {
      LOGGER.error("Register write requested but could not understand the request {}", commandContent);
      return gdbIo.generateUnsupportedResponse();
    }
  }

  public String readAllRegisters() {
    LOGGER.info("Reading all registers");
    StringBuilder response = new StringBuilder(2 * 4 * 16);
    for (int i = 0; i < 16; i++) {
      String regValue = gdbFormatter.formatValueAsHex32(getRegisterValue(i));
      response.append(regValue);
    }
    return gdbIo.generateResponse(response.toString());
  }

  private int getRegisterValue(int regIndex) {
    State state = machine.getCpu().getState();
    if (regIndex < 8) {
      return state.getRegisters().getRegister(regIndex);
    }
    if (regIndex == 8) {
      // GDB does not take CS into account as it does not support real mode ...
      return state.getIpPhysicalAddress();
    }
    if (regIndex == 9) {
      return state.getFlags().getFlagRegister();
    }
    if (regIndex < 16) {
      return state.getSegmentRegisters().getRegister(getSegmentRegisterIndex(regIndex));
    }
    return 0;
  }

  private void setRegisterValue(int regIndex, int value) {
    State state = machine.getCpu().getState();
    if (regIndex < 8) {
      state.getRegisters().setRegister(regIndex, value);
    } else if (regIndex == 8) {
      state.setIP(value);
    } else if (regIndex == 9) {
      state.getFlags().setFlagRegister(value);
    } else if (regIndex < 16) {
      state.getSegmentRegisters().setRegister(getSegmentRegisterIndex(regIndex), value);
    }
  }

  private int getSegmentRegisterIndex(int gdbRegisterIndex) {
    int registerIndex = gdbRegisterIndex - 10;
    // in hardware register order is ES, CS, SS, DS, FS, GS but in GDB it seems to be CS, SS, DS, ES, FS, GS
    if (registerIndex < 3) {
      // shift Below ES
      return registerIndex + 1;
    }
    if (registerIndex == 3) {
      // remap for ES
      return 0;
    }
    return registerIndex;
  }
}
