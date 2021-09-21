package spice86.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import spice86.emulator.Configuration;
import spice86.emulator.ProgramExecutor;
import spice86.emulator.function.OverrideSupplier;

/**
 * GUI entry point.<br/>
 * Responsible for setting up java-fx and starting the exe from the configuration provided in the command line.
 */
public class Spice86Application extends Application {
  private static final Logger LOGGER = LoggerFactory.getLogger(Spice86Application.class);

  private ProgramExecutor programExecutor;

  private Configuration generateConfiguration() {
    return new CommandLineParser().parseCommandLine(this.getParameters());
  }

  @Override
  public void start(Stage stage) {
    Configuration configuration = generateConfiguration();
    if (configuration == null) {
      exit();
    }
    Gui gui = new Gui();
    gui.setStage(stage);
    gui.setResolution(320, 200);
    stage.setTitle("Spice86: " + configuration.getExe());
    stage.setResizable(false);
    stage.setOnCloseRequest(event -> exit());
    stage.setOnShown(event -> startMachine(gui, configuration));
    stage.getIcons()
        .add(new Image(Spice86Application.class.getResourceAsStream("/icon.png")));
    stage.show();
  }

  private void startMachine(Gui gui, Configuration configuration) {
    Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "Executor")).execute(() -> {
      try {
        programExecutor = new ProgramExecutor(gui, configuration);
        programExecutor.run();
      } catch (Exception e) {
        LOGGER.error("An error occurred during execution", e);
      }
      exit();
    });
  }

  private void exit() {
    Optional.ofNullable(programExecutor).ifPresent(ProgramExecutor::close);
    Platform.exit();
    System.exit(0);
  }

  public static void main(String[] args) {
    launch(args);
  }

  public static <T extends OverrideSupplier> void runWithOverrides(String[] args, Class<T> overrides, String expectedChecksum) {
    List<String> argsList = new ArrayList<>(Arrays.asList(args));
    // Inject override
    argsList.add("--overrideSupplierClassName=" + overrides.getCanonicalName());
    argsList.add("--expectedChecksum=" + expectedChecksum);
    main(argsList.toArray(new String[argsList.size()]));
  }
}
