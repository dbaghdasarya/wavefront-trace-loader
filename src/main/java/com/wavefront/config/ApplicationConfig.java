package com.wavefront.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Application configuration object for setting sender related information.
 *
 * @author Sirak Ghazaryan (sghazaryan@vmware.com)
 */
public class ApplicationConfig {
  /**
   * Saving to file has the highest priority.
   */
  @JsonProperty
  private String outputFile = null;
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
  private Integer distributionPort = 30000;
  /**
   * Port used for tracing.
   */
  @JsonProperty
  private Integer tracingPort = 40000;
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


  public String getOutputFile() {
    return outputFile;
  }

  public String getServer() {
    return server;
  }

  public String getToken() {
    return token;
  }

  public String getProxyServer() {
    return proxyServer;
  }

  public Integer getMetricsPort() {
    return metricsPort;
  }

  public Integer getDistributionPort() {
    return distributionPort;
  }

  public Integer getTracingPort() {
    return tracingPort;
  }
}
