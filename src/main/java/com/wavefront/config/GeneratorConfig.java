package com.wavefront.config;

import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wavefront.DataQueue;
import com.wavefront.datastructures.TraceTypePattern;
import com.wavefront.generators.FromPatternGenerator;
import com.wavefront.generators.FromTopologyGenerator;
import com.wavefront.generators.TraceGenerator;
import com.wavefront.helpers.Defaults;
import com.wavefront.helpers.DurationStringConverter;
import com.wavefront.topology.TraceTopology;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Configuration stores settings for generating spans.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 * @author Sirak Ghazaryan (sghazaryan@vmware.com)
 */
@SuppressWarnings("unused, FieldCanBeLocal")
public class GeneratorConfig {
  private static final Logger LOGGER = Logger.getLogger(GeneratorConfig.class.getCanonicalName());

  @Parameter(names = {"--help"}, help = true)
  private boolean help = false;

  @Parameter(names = {"--configFile"}, description = "Location of application config yaml file.")
  private String appConfigFile = "applicationConfig.yaml";

  @Parameter(names = {"--rate"}, description = "Rate at which the spans will be ingested (spans " +
      "per second).")
  private Integer spansRate = 100;

  @Parameter(names = {"--traceTypesCount"}, description = "Number of traces types for " +
      "auto-generation.")
  private Integer traceTypesCount = 3;

  @Parameter(names = {"--totalTraceCount"}, description = "Total number of traces for " +
      "generation. This parameter disables duration")
  private Integer totalTraceCount = 0;

  @Parameter(names = {"--errorRate"}, description = "Percentage of erroneous traces.")
  private Integer errorRate = 0;

  @Parameter(names = {"--debugRate"}, description = "Percentage of debug spans in traces.")
  private Integer debugRate = 0;

  @Parameter(names = {"--duration"}, description = "Duration of ingestion time (00h00m00s).",
      converter = DurationStringConverter.class)
  private Duration duration = Duration.ofMinutes(5);

  @Parameter(names = {"-f", "--file"}, description = "Generator config file.", order = 0)
  private String generatorConfigFile = null;

  @Parameter(names = {"--stat"}, description = "Output file to dump statistics about generated " +
      "traces.")
  private String statisticsFile = null;

  @Parameter
  private List<String> unparsedParams;

  private LinkedList<TraceTypePattern> traceTypePatterns;
  private TraceTopology traceTopology;

  public GeneratorConfig() {
  }

  public void initPropertiesFromFile() throws IOException {
    // read json file data to String
    byte[] jsonData = Files.readAllBytes(Paths.get(generatorConfigFile));
    // create ObjectMapper instance
    ObjectMapper objectMapper = new ObjectMapper();

    // read JSON like DOM Parser
    JsonNode rootNode = objectMapper.readTree(jsonData);
    spansRate = rootNode.path("spansRate").asInt();
    duration = (new DurationStringConverter()).convert(rootNode.path("duration").asText());
    errorRate = rootNode.path("errorRate").asInt(0);
    debugRate = rootNode.path("debugRate").asInt(0);
    // if traceTypesCount is set, it has precedence so the traces will be generated with default
    // parameters
    traceTypesCount = rootNode.path("traceTypesCount").asInt(0);
    totalTraceCount = rootNode.path("totalTraceCount").asInt(0);

    if (traceTypesCount <= 0) {
      if (rootNode.findValue("traceTypePatterns") != null) {
        traceTypePatterns =
            objectMapper.readValue(
                rootNode.path("traceTypePatterns").toString(),
                new TypeReference<LinkedList<TraceTypePattern>>() {
                });
      }
      if (rootNode.findValue("traceTopology") != null) {
        traceTopology =
            objectMapper.readValue(
                rootNode.path("traceTopology").toString(), new TypeReference<TraceTopology>() {
                });
      }
    }
  }

  /**
   * Constructs traceTypePatterns list based on traceTypesCount input parameter. If it's not set or
   * smaller than 0, this function does nothing.
   */
  public void initMissingPropertiesWithDefaults() {
    if (traceTypesCount <= 0) {
      return;
    }
    // generate traces with default parameters
    traceTypePatterns = new LinkedList<>();
    Random rand = new Random(System.currentTimeMillis());
    for (int n = 0; n < traceTypesCount; n++) {
      traceTypePatterns.add(
          new TraceTypePattern(
              Defaults.DEFAULT_TYPE_NAME_PREFIX + (n + 1),
              Defaults.DEFAULT_SPAN_NAME_SUFFIX,
              Defaults.DEFAULT_NESTING_LEVEL,
              100 / traceTypesCount,
              Defaults.DEFAULT_SPANS_DISTRIBUTIONS,
              Defaults.DEFAULT_TRACE_DURATIONS,
              Defaults.DEFAULT_MANDATORY_TAGS,
              errorRate,
              debugRate));
    }
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

  public Duration getDuration() {
    return duration;
  }

  public String getGeneratorConfigFile() {
    return generatorConfigFile;
  }

  public void setGeneratorConfigFile(String generatorConfigFile) {
    this.generatorConfigFile = generatorConfigFile;
  }

  public List<String> getUnparsedParams() {
    return unparsedParams;
  }

  public LinkedList<TraceTypePattern> getTraceTypePatterns() {
    return traceTypePatterns;
  }

  public TraceTopology getTraceTopology() {
    return traceTopology;
  }

  public Integer getTotalTraceCount() {
    return totalTraceCount;
  }

  public String getStatisticsFile() {
    return statisticsFile;
  }

  public TraceGenerator getGenerator(DataQueue dataQueue) {
    if (traceTypePatterns != null) {
      return new FromPatternGenerator(this, dataQueue);
    } else if (traceTopology != null) {
      return new FromTopologyGenerator(this, dataQueue);
    }

    return null;
  }
}