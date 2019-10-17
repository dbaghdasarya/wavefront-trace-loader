package com.wavefront.config;

import com.beust.jcommander.Parameter;

import java.util.List;
import java.util.logging.Logger;

/**
 * Configuration stores settings for generating spans.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 * @author Sirak Ghazaryan (sghazaryan@vmware.com)
 */
public class GeneratorConfig {
  private static final Logger logger = Logger.getLogger(GeneratorConfig.class.getCanonicalName());

  @Parameter(names = {"--help"}, help = true)
  private boolean help = false;

  @Parameter(names = {"--configFile"}, description = "Location of application config yaml file.")
  protected String appConfigFile = "applicationConfig.yaml";

  @Parameter(names = {"--rate"}, description = "Secondly rate at which the spans will be ingested.")
  protected Integer spansRate = 0;

  @Parameter(names = {"--duration"}, description = "Duration of ingestion time in minutes.")
  protected Integer duration = 0;

  @Parameter(names = {"-f", "--file"}, description = "Generator config file.", order = 0)
  private String generatorConfigFile = null;

  @Parameter(description = "")
  protected List<String> unparsedParams;

  public GeneratorConfig() {
  }

  public void initPropertiesFromFile() {
    // TODO handle reading from file.
    logger.info("initPropertiesFromFile() function is called.");
  }

  public boolean isHelp() {
    return help;
  }

  public String getAppConfigFile() {
    return appConfigFile;
  }

  public Integer getSpansRate() {
    return spansRate;
  }

  public Integer getDuration() {
    return duration;
  }

  public String getGeneratorConfigFile() {
    return generatorConfigFile;
  }

  public List<String> getUnparsedParams() {
    return unparsedParams;
  }
}
