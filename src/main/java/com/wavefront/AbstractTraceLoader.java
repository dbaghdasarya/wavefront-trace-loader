package com.wavefront;

import com.google.common.base.Joiner;

import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.wavefront.config.ApplicationConfig;
import com.wavefront.config.GeneratorConfig;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Abstract Trace Loader class.
 *
 * @author Sirak Ghazaryan (sghazaryan@vmware.com)
 */
public abstract class AbstractTraceLoader {
  protected static final Logger LOGGER = Logger.getLogger("traceloader");
  protected GeneratorConfig generatorConfig = new GeneratorConfig();

  private void parseArguments(String[] args) {
    LOGGER.info("Arguments: " + Arrays.stream(args).
        collect(Collectors.joining(", ")));
    JCommander jCommander = JCommander.newBuilder().
        programName(this.getClass().getCanonicalName()).
        addObject(generatorConfig).
        allowParameterOverwriting(true).
        build();
    jCommander.parse(args);
    if (generatorConfig.isHelp()) {
      jCommander.usage();
      System.exit(0);
    }
    if (generatorConfig.getUnparsedParams() != null) {
      LOGGER.info("Unparsed arguments: " + Joiner.on(", ").
          join(generatorConfig.getUnparsedParams()));
    }
  }

  /**
   * Entry-point for the application.
   *
   * @param args Command-line parameters passed on to JCommander to configure the daemon.
   */
  public void start(String[] args) throws IOException {
    try {
      // Parse commandline arguments
      parseArguments(args);

      loadGeneratorConfigurationFile();
      setupSenders();
      setupGenerators();
      generateSpans();
      sendSpans();
      dumpStatistics();
    } catch (Throwable t) {
      LOGGER.log(Level.SEVERE, "Aborting start-up", t);
      System.exit(1);
    }
  }

  private void loadGeneratorConfigurationFile() throws IOException {
    // If they've specified a configuration file, override the command line values
    try {
      if (generatorConfig.getGeneratorConfigFile() != null) {
        generatorConfig.initPropertiesFromFile();
      }
      generatorConfig.initMissingPropertiesWithDefaults();

    } catch (Throwable e) {
      LOGGER.severe("Could not load generator configuration file " + generatorConfig.getGeneratorConfigFile());
      throw e;
    }
  }

  protected ApplicationConfig loadApplicationConfig() throws IOException {
    try {
      if (generatorConfig.getAppConfigFile() == null) {
        return null;
      }
      ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
      return objectMapper.readValue(new File(generatorConfig.getAppConfigFile()), ApplicationConfig.class);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Could not load application config", e);
      throw e;
    }
  }

  abstract void setupSenders() throws IOException;

  abstract void generateSpans();

  abstract void sendSpans() throws Exception;

  abstract void dumpStatistics() throws Exception;

  abstract void setupGenerators() throws Exception;
}
