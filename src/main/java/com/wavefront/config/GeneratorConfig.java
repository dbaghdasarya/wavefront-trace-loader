package com.wavefront.config;

import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wavefront.TraceTypePattern;
import com.wavefront.helpers.DurationStringConverter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import javax.print.attribute.IntegerSyntax;

/**
 * Configuration stores settings for generating spans.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 * @author Sirak Ghazaryan (sghazaryan@vmware.com)
 */
@SuppressWarnings("unused, FieldCanBeLocal")
public class GeneratorConfig {
  private static final Logger logger = Logger.getLogger(GeneratorConfig.class.getCanonicalName());

  @Parameter(names = {"--help"}, help = true)
  private boolean help = false;

  @Parameter(names = {"--configFile"}, description = "Location of application config yaml file.")
  private String appConfigFile = "applicationConfig.yaml";

  @Parameter(names = {"--rate"}, description = "Rate at which the traces will be ingested.")
  private Double tracesRate = 0.0;

  @Parameter(names = {"--traceTypesNumber"}, description = "Number of traces types for " +
      "auto-generation.")
  private Integer traceTypesNumber = 0;

  @Parameter(names = {"--errorRate"}, description = "Percentage of erroneous traces.")
  private Integer errorRate = 0;

  @Parameter(names = {"--duration"}, description = "Duration of ingestion time in minutes.",
      converter = DurationStringConverter.class)
  private Duration duration;

  @Parameter(names = {"-f", "--file"}, description = "Generator config file.", order = 0)
  private String generatorConfigFile = null;

  @Parameter
  private List<String> unparsedParams;

  private LinkedList<TraceTypePattern> traceTypes;

  public GeneratorConfig() {
  }

  public void initPropertiesFromFile() throws IOException {
    logger.info("initPropertiesFromFile() function is called.");

    //read json file data to String
    byte[] jsonData = Files.readAllBytes(Paths.get(getGeneratorConfigFile()));
    //create ObjectMapper instance
    ObjectMapper objectMapper = new ObjectMapper();

    //read JSON like DOM Parser
    JsonNode rootNode = objectMapper.readTree(jsonData);
    tracesRate = rootNode.path("tracesRate").asDouble();
    duration = (new DurationStringConverter()).convert(rootNode.path("duration").asText());
    errorRate = rootNode.path("errorRate").asInt();
    traceTypesNumber = rootNode.path("traceTypesNumber").asInt(0);
    if (traceTypesNumber <= 0) {
      traceTypes = objectMapper.readValue(rootNode.path("traceTypes").toString(),
          new TypeReference<LinkedList<TraceTypePattern>>() {
          });
    } else {
      traceTypes = new LinkedList<>();
      Random rand = new Random();
      for (int n = 0; n < traceTypesNumber; n++) {
        // TODO: tracesNumber should be calculated more accurately
        traceTypes.add(new TraceTypePattern("traceType_" + n,
            rand.nextInt(6) + 4,
            rand.nextInt(20) + 4,
            100,
            errorRate));
      }
    }
  }

  public boolean isHelp() {
    return help;
  }

  public String getAppConfigFile() {
    return appConfigFile;
  }

  public Double getTracesRate() {
    return tracesRate;
  }

  public Duration getDuration() {
    return duration;
  }

  public String getGeneratorConfigFile() {
    return generatorConfigFile;
  }

  public List<String> getUnparsedParams() {
    return unparsedParams;
  }

  public LinkedList<TraceTypePattern> getTraceTypes() {
    return traceTypes;
  }
}
