package spice86.emulator.devices.timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spice86.emulator.Configuration;
import spice86.emulator.cpu.State;

/**
 * Configurator for Timer counters. Will decide to use time based or instruction based Activator depending on the
 * configuration.
 */
public class CounterConfigurator {
  private static final Logger LOGGER = LoggerFactory.getLogger(CounterConfigurator.class);

  private static final long DEFAULT_INSTRUCTIONS_PER_SECONDS = 2_000_000l;
  private Configuration configuration;

  public CounterConfigurator(Configuration configuration) {
    this.configuration = configuration;
  }

  public CounterActivator instanciateCounterActivator(State state) {
    Long instructionsPerSecond = configuration.getInstructionsPerSecond();
    if (instructionsPerSecond == null && configuration.getGdbPort() != null) {
      // With GDB, force to instructions per seconds as time based timers could perturbate steps
      instructionsPerSecond = DEFAULT_INSTRUCTIONS_PER_SECONDS;
      LOGGER.warn("Forcing Counter to use instructions per seconds since in GDB mode. "
          + "If speed is too slow or too fast adjust the --instructionsPerSecond parameter");
    }
    if (instructionsPerSecond != null) {
      return new CyclesCounterActivator(state, instructionsPerSecond);
    }
    return new TimeCounterActivator(configuration.getTimeMultiplier());
  }
}
