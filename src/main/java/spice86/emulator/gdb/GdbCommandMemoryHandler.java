package spice86.emulator.gdb;

import static spice86.utils.ConvertUtils.hexToByteArray;
import static spice86.utils.ConvertUtils.parseHex32;
import static spice86.utils.ConvertUtils.uint32i;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.machine.Machine;
import spice86.emulator.memory.Memory;
import spice86.utils.ConvertUtils;

public class GdbCommandMemoryHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(GdbCommandMemoryHandler.class);
  private GdbIo gdbIo;
  private Machine machine;
  private GdbFormatter gdbFormatter = new GdbFormatter();

  public GdbCommandMemoryHandler(GdbIo gdbIo, Machine machine) {
    this.gdbIo = gdbIo;
    this.machine = machine;
  }

  public String writeMemory(String commandContent) {
    try {
      String[] commandContentSplit = commandContent.split("[,:]");
      long address = parseHex32(commandContentSplit[0]);
      long length = parseHex32(commandContentSplit[1]);
      byte[] data = hexToByteArray(commandContentSplit[2]);
      if (length != data.length) {
        return gdbIo.generateResponse("E01");
      }
      Memory memory = machine.getMemory();
      if (address + length > memory.getSize()) {
        return gdbIo.generateResponse("E02");
      }
      memory.loadData(uint32i(address), data);
      return gdbIo.generateResponse("OK");
    } catch (NumberFormatException nfe) {
      LOGGER.error("Memory write requested but could not understand the request {}", commandContent);
      return gdbIo.generateUnsupportedResponse();
    }
  }

  public String readMemory(String commandContent) {
    try {
      String[] commandContentSplit = commandContent.split(",");
      long address = parseHex32(commandContentSplit[0]);
      long length = 1;
      if (commandContentSplit.length > 1) {
        length = parseHex32(commandContentSplit[1]);
      }
      LOGGER.info("Reading memory at address {} for a length of {}", address, length);
      Memory memory = machine.getMemory();
      int memorySize = memory.getSize();
      if (address < 0) {
        return gdbIo.generateResponse("");
      }
      StringBuilder response = new StringBuilder(uint32i(length * 2));
      for (long i = 0; i < length; i++) {
        long readAddress = address + i;
        if (readAddress >= memorySize) {
          break;
        }
        int b = memory.getUint8(uint32i(readAddress));
        String value = gdbFormatter.formatValueAsHex8(b);
        response.append(value);
      }
      return gdbIo.generateResponse(response.toString());
    } catch (NumberFormatException nfe) {
      LOGGER.error("Memory read requested but could not understand the request {}", commandContent);
      return gdbIo.generateUnsupportedResponse();
    }
  }

  @SuppressWarnings({
      // False positive, there is a semicolon in the comments but it's not commented code...
      "java:S125"
  })
  public String searchMemory(String command) {
    String[] parameters = command.replace("Search:memory:", "").split(";");
    long start = ConvertUtils.parseHex32(parameters[0]);
    long end = ConvertUtils.parseHex32(parameters[1]);
    // read the bytes from the raw command as GDB does not send them as hex
    List<Byte> rawCommand = gdbIo.getRawCommand();
    // Extract the original hex sent by GDB, read from
    // 3: +$q
    // variable: header
    // 2: ;
    // variable 2 hex strings
    int patternStartIndex = 3 + "Search:memory:".length() + 2 + parameters[0].length() + parameters[1].length();
    List<Byte> patternBytesList = rawCommand.subList(patternStartIndex, rawCommand.size() - 1);
    Memory memory = machine.getMemory();
    Integer address = memory.searchValue((int)start, (int)end, patternBytesList);
    if (address == null) {
      return gdbIo.generateResponse("0");
    }
    return gdbIo.generateResponse("1," + gdbFormatter.formatValueAsHex32(address));
  }
}
