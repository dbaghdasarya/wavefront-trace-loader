package com.wavefront;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.wavefront.config.ApplicationConfig;
import com.wavefront.config.GeneratorConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
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
  protected final GeneratorConfig generatorConfig = new GeneratorConfig();
  protected ApplicationConfig applicationConfig;

  @VisibleForTesting
  protected void parseArguments(String[] args) {
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
  public void start(String[] args) {
    try {
      loadApplicationConfig();
      cycle(args);
    } catch (Throwable t) {
      handleProgramExit(t);
    }
  }

  private void cycle(String[] args) {
    try {
      int cycle = Integer.parseInt(applicationConfig.getCycle());
      for (int i = 0; i < cycle; i++) {
        executeOneIteration(args);
      }
    } catch (NumberFormatException e) {
      while (true) {
        executeOneIteration(args);
      }
    }
  }

  private void executeOneIteration(String[] args) {
    List<String> inputJsonFiles = applicationConfig.getInputJsonFiles();
    if (inputJsonFiles == null || inputJsonFiles.isEmpty()) {
      try {
        if (args.length == 0) {
          LOGGER.info("Since neither Command Line Arguments, nor YAML config files are provided " +
              "the default option will be executed.");
        }
        // Parse commandline arguments.
        parseArguments(args);
        start();
      } catch (ParameterException e) {
        LOGGER.log(Level.SEVERE, "Generation can't be started! Neither Command Line Arguments, " +
            "nor YAML config files are provided.");
        LOGGER.info("Arguments: " + Arrays.stream(args).collect(Collectors.joining(", ")) + " are " +
            "not enough to start generating.");
        System.exit(1);
      }
    } else {
      LOGGER.log(Level.INFO, "Please, be aware that if provided Command Line Arguments will be " +
          "ignored!");
      inputJsonFiles.forEach(inputJsonFile -> {
        generatorConfig.setGeneratorConfigFile(inputJsonFile);
        start();
      });
    }
  }

  private void start() {
    try {
      loadGeneratorConfigurationFile();

      initialize();

      setupSenders();
      setupGenerators();

      startLoading();

      dumpStatistics();
    } catch (Throwable t) {
      handleProgramExit(t);
    }
  }

  private void handleProgramExit(Throwable t) {
    LOGGER.log(Level.SEVERE, "Aborting start-up", t);
    System.exit(1);
  }

  private void loadGeneratorConfigurationFile() throws Exception {
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

  @VisibleForTesting
  protected void loadApplicationConfig() throws Exception {
    try {
      if (generatorConfig.getAppConfigFile() == null) {
        throw new Exception("Application config can be loaded only after proper loading of " +
            "generator file!");
      }
      ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
      if (applicationConfig == null) {
        applicationConfig = objectMapper.readValue(new File(generatorConfig.getAppConfigFile()),
            ApplicationConfig.class);
      }
      if (applicationConfig.getTraceOutputFile() != null) {
        new FileWriter(applicationConfig.getTraceOutputFile(), false).close();
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Could not load application config", e);
      throw e;
    }
  }

  abstract void initialize();

  abstract void setupSenders() throws IOException;

  abstract void startLoading() throws Exception;

  abstract void dumpStatistics() throws Exception;

  abstract void setupGenerators() throws Exception;
}
