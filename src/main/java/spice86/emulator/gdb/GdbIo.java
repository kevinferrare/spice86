package spice86.emulator.gdb;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spice86.utils.ConvertUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles communication with GDB
 */
public class GdbIo implements java.io.Closeable {
  private static final Logger LOGGER = LoggerFactory.getLogger(GdbIo.class);

  private ServerSocket serverSocket;
  private Socket socket;
  private InputStream input;
  private OutputStream output;
  private List<Byte> rawCommand = new ArrayList<>();
  private GdbFormatter gdbFormatter = new GdbFormatter();

  public GdbIo(int port) throws IOException {
    LOGGER.info("GDB Server listening on port {}", port);
    serverSocket = new ServerSocket(port);
    socket = serverSocket.accept();
    LOGGER.info("Client connected: {}", socket.getInetAddress().getCanonicalHostName());
    input = socket.getInputStream();
    output = socket.getOutputStream();
  }

  public List<Byte> getRawCommand() {
    return rawCommand;
  }

  public String readCommand() throws IOException {
    rawCommand.clear();
    int chr = input.read();
    StringBuilder resBuilder = new StringBuilder();
    while (chr >= 0) {
      rawCommand.add((byte)chr);
      if ((char)chr == '#') {
        // checksum, ignored...
        input.readNBytes(2);
        break;
      } else {
        resBuilder.append((char)chr);
      }
      chr = input.read();
    }
    return getPayload(resBuilder);
  }

  private String getPayload(StringBuilder resBuilder) {
    String res = resBuilder.toString();
    int beginning = res.indexOf('$');
    if (beginning != -1) {
      return StringUtils.substring(res, beginning + 1);
    }
    beginning = res.indexOf('+');
    if (beginning != -1) {
      return StringUtils.substring(res, beginning + 1);
    }
    return res;
  }

  public void sendResponse(String data) throws IOException {
    if (data != null) {
      LOGGER.info("Sending response {}", data);
      output.write(data.getBytes(StandardCharsets.UTF_8));
    }
  }

  public String generateMessageToDisplayResponse(String message) {
    String toSend = message + '\n';
    return this.generateResponse(ConvertUtils.byteArrayToHexString(toSend.getBytes()));
  }

  public String generateResponse(String data) {
    int checksum = 0;
    for (byte b : data.getBytes()) {
      checksum += b;
    }
    return "+$" + data + '#' + gdbFormatter.formatValueAsHex8(checksum);
  }

  public String generateUnsupportedResponse() {
    return "";
  }

  @Override
  public void close() throws IOException {
    serverSocket.close();
    socket.close();
  }

}
