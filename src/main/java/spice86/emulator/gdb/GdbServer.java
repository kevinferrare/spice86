package spice86.emulator.gdb;

import java.io.IOException;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.machine.Machine;

/**
 * GDB server for the code being executed.
 */
public class GdbServer implements java.io.Closeable {
  private static final Logger LOGGER = LoggerFactory.getLogger(GdbServer.class);
  private Machine machine;
  private boolean running = true;
  private volatile boolean started = false;
  private String defaultDumpDirectory;

  public GdbServer(Machine machine, int port, String defaultDumpDirectory) {
    this.machine = machine;
    this.defaultDumpDirectory = defaultDumpDirectory;
    start(port);
  }

  @Override
  public void close() {
    running = false;
  }

  private void start(int port) {
    Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "GdbServer"))
        .execute(() -> runServer(port));
    // wait for thread to start
    while (!started)
      ;
  }

  private void runServer(int port) {
    LOGGER.info("Starting GDB server");
    try {
      while (running) {
        try (GdbIo gdbIo = new GdbIo(port)) {
          acceptOneConnection(gdbIo);
        } catch (IOException e) {
          LOGGER.error("Error in the GDB server, restarting it...", e);
        }
      }
    } finally {
      // gracefully stop
      machine.getCpu().setRunning(false);
      // Resume if needed
      machine.getMachineBreakpoints().getPauseHandler().requestResume();
      LOGGER.info("GDB server stopped");
    }
  }

  private void acceptOneConnection(GdbIo gdbIo) throws IOException {
    GdbCommandHandler gdbCommandHandler = new GdbCommandHandler(gdbIo, machine, defaultDumpDirectory);
    // Pause the CPU waiting for GDB to connect
    gdbCommandHandler.pauseEmulator();
    this.started = true;
    while (gdbCommandHandler.isConnected()) {
      String command = gdbIo.readCommand();
      if (StringUtils.isNotEmpty(command)) {
        gdbCommandHandler.runCommand(command);
      }
    }
  }
}
