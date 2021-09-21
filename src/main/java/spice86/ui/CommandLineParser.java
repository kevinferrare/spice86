package spice86.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import spice86.emulator.Configuration;
import spice86.emulator.errors.UnrecoverableException;
import spice86.emulator.function.OverrideSupplier;

/**
 * Parses the command line options to create a Configuration.<br/>
 * Displays help when configuration could not be parsed.
 */
public class CommandLineParser {
  private static final Logger LOGGER = LoggerFactory.getLogger(CommandLineParser.class);

  private static final int DEFAULT_INSTRUCTIONS_PER_SECOND = 5_000_000;
  private static final int DEFAULT_PROGRAM_START_SEGMENT = 0x01ED;

  private String getCDrive(String value) {
    if (StringUtils.isEmpty(value)) {
      return "./";
    }
    String unixPathValue = value.replace('\\', '/');
    if (!unixPathValue.endsWith("/")) {
      unixPathValue += "/";
    }
    return unixPathValue;
  }

  private long parseInstructionsPerSecondParameter(String value) {
    if (NumberUtils.isDigits(value)) {
      return Long.parseLong(value);
    }
    return DEFAULT_INSTRUCTIONS_PER_SECOND;
  }

  private Integer parseInt(String value) {
    if (NumberUtils.isDigits(value)) {
      return Integer.parseInt(value);
    }
    return null;
  }

  private OverrideSupplier parseFunctionInformationSupplierClassName(String supplierClassName) {
    if (supplierClassName == null) {
      return null;
    }
    try {
      @SuppressWarnings("unchecked")
      Class<OverrideSupplier> supplierClass =
          (Class<OverrideSupplier>)Class.forName(supplierClassName);
      if (!OverrideSupplier.class.isAssignableFrom(supplierClass)) {
        String error =
            "Provided class " + supplierClassName + " does not implement the "
                + OverrideSupplier.class.getCanonicalName() + " interface ";
        throw new UnrecoverableException(error);
      }
      return supplierClass.getDeclaredConstructor().newInstance();
    } catch (ClassNotFoundException exception) {
      throw new UnrecoverableException("Could not load provided class " + supplierClassName, exception);
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
        | NoSuchMethodException | SecurityException exception) {
      throw new UnrecoverableException("Could not instantiate provided class " + supplierClassName, exception);
    }
  }

  private String getExe(Application.Parameters parameters) {
    List<String> unNamedParameters = parameters.getUnnamed();
    if (CollectionUtils.isEmpty(unNamedParameters)) {
      return null;
    }
    return IterableUtils.first(unNamedParameters);
  }

  private boolean parseUseFunctionOverride(String value) {
    Boolean booleanValue = BooleanUtils.toBooleanObject(value);
    // By default if null will return true
    return BooleanUtils.isNotFalse(booleanValue);
  }

  private int parseProgramEntryPointSegment(String value) {
    Integer segment = parseInt(value);
    if (segment == null) {
      return DEFAULT_PROGRAM_START_SEGMENT;
    }
    return segment;
  }

  @SuppressWarnings("java:S106")
  public Configuration parseCommandLine(Application.Parameters parameters) {
    Configuration configuration = new Configuration();
    configuration.setExe(getExe(parameters));
    if (StringUtils.isEmpty(configuration.getExe())) {
      LOGGER.info(
          """
              Parameters:
              <path to exe>
              --cDrive=<path to C drive, default is .>
              --instructionsPerSecond=<number of instructions executed in by the emulator in a second on your machine. Default is \
              """
              + DEFAULT_INSTRUCTIONS_PER_SECOND
              + """
                  >
                  --gdbPort=<gdb port, if empty gdb server will not be created. If not empty, application will pause until gdb connects>
                  --overrideSupplierClassName=<Name of a class in the classpath that will generate the initial function informations. See documentation for more information.>
                  --useCodeOverride=<true or false> if false it will use the names provided by overrideSupplierClassName but not the code
                  --programEntryPointSegment=<Segment where to load the program. DOS PSP and MCB will be created before it>""");
      return null;
    }
    Map<String, String> commandLineParameters = parameters.getNamed();
    configuration.setcDrive(getCDrive(commandLineParameters.get("cDrive")));
    configuration.setInstructionsPerSecond(
        parseInstructionsPerSecondParameter(commandLineParameters.get("instructionsPerSecond")));
    configuration.setGdbPort(parseInt(commandLineParameters.get("gdbPort")));
    configuration.setOverrideSupplier(
        this.parseFunctionInformationSupplierClassName(
            commandLineParameters.get("overrideSupplierClassName")));
    configuration
        .setUseCodeOverride(this.parseUseFunctionOverride(commandLineParameters.get("useCodeOverride")));
    configuration.setInstallInterruptVector(true);
    configuration.setProgramEntryPointSegment(
        this.parseProgramEntryPointSegment(commandLineParameters.get("programEntryPointSegment")));
    return configuration;
  }

}
