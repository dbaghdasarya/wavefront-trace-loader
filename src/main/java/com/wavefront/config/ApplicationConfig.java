package com.wavefront.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.wavefront.helpers.ApplicationConfigValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * Application configuration object for setting sender related information.
 *
 * @author Sirak Ghazaryan (sghazaryan@vmware.com)
 */
@JsonDeserialize(converter = ApplicationConfigValidator.class)
public class ApplicationConfig {
  /**
   * Input file name for parse Wavefront traces from file and re-ingest them.
   */
  @JsonProperty
  private String wfTracesFile;
  /**
   * Number of times the program should execute.
   */
  @JsonProperty
  private String cycle = "1";
  /**
   * Output file name for exporting the plain list of spans. Saving to file has the highest
   * priority.
   */
  @JsonProperty
  private String spanOutputFile = null;
  /**
   * Output file name for exporting consistent traces. Saving to file has the highest priority.
   */
  @JsonProperty
  private String traceOutputFile = null;
  /**
   * Pattern/topology Trace Type files
   */
  @JsonProperty
  private List<String> inputJsonFiles = new ArrayList<>();
  /**
   * Proxy server for routing traffic to wavefront. If it's set, the direct ingestion configs will
   * be ignored.
   */
  @JsonProperty
  private String proxyServer = null;
  /**
   * Metrics port.
   */
  @JsonProperty
  private Integer metricsPort = 2878;
  /**
   * Port used for distribution.
   */
  @JsonProperty
  private Integer distributionPort = 40000;
  /**
   * Port used for tracing.
   */
  @JsonProperty
  private Integer tracingPort = 30000;
  /**
   * Port used for additional tracing data.
   */
  @JsonProperty
  private Integer customTracingPorts = 30001;
  /**
   * Server URL used for direct ingestion.
   */
  @JsonProperty
  private String server = null;
  /**
   * Token to auto-register proxy with an account, used for direct ingestion only.
   */
  @JsonProperty
  private String token = null;


  public String getWfTracesFile() {
    return wfTracesFile;
  }

  public String getCycle() {
    return cycle;
  }

  public void setCycle(String cycle) {
    this.cycle = cycle;
  }

  public String getSpanOutputFile() {
    return spanOutputFile;
  }

  public String getTraceOutputFile() {
    return traceOutputFile;
  }

  public String getServer() {
    return server;
  }

  public String getToken() {
    return token;
  }

  public String getProxyServer() { return proxyServer; }

  public List<String> getInputJsonFiles() { return inputJsonFiles; }

  public Integer getMetricsPort() {
    return metricsPort;
  }

  public Integer getDistributionPort() {
    return distributionPort;
  }

  public Integer getTracingPort() {
    return tracingPort;
  }

  public Integer getCustomTracingPorts() {
    return customTracingPorts;
  }
}