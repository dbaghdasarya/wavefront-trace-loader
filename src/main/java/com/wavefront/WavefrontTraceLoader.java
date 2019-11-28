package com.wavefront;

import com.google.common.base.Strings;

import com.wavefront.config.ApplicationConfig;
import com.wavefront.sdk.common.WavefrontSender;
import com.wavefront.sdk.direct.ingestion.WavefrontDirectIngestionClient;
import com.wavefront.sdk.proxy.WavefrontProxyClient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Input point of application to generate and send traces to wavefront.
 *
 * @author Sirak Ghazaryan (sghazaryan@vmware.com)
 */
public class WavefrontTraceLoader extends AbstractTraceLoader {
  private SpanGenerator spanGenerator;
  private SpanSender spanSender;
  private SpanQueue spanQueue;

  public static void main(String[] args) throws IOException {
    new WavefrontTraceLoader().start(args);
  }

  @Override
  void setupSenders() throws IOException {
    ApplicationConfig config = loadApplicationConfig();
    if (config == null) {
      throw new IOException("Application config should contain proxy or direction ingestion info.");
    }
    // TODO do we need additional checks here??
    if (!Strings.isNullOrEmpty(config.getOutputFile())) {
      spanSender = new SpanSender(config.getOutputFile());
    } else {
      WavefrontSender wavefrontSender;
      if (config.getProxyServer() != null) {
        wavefrontSender = new WavefrontProxyClient.Builder(config.getProxyServer()).
            metricsPort(config.getMetricsPort()).
            distributionPort(config.getDistributionPort()).
            tracingPort(config.getTracingPort()).build();
      } else {
        wavefrontSender = new WavefrontDirectIngestionClient.Builder(config.getServer(), config.getToken()).build();
      }

      spanSender = new SpanSender(wavefrontSender, generatorConfig.getSpansRate());
    }
  }

  @Override
  void setupGenerators() {
    this.spanGenerator = new SpanGenerator(generatorConfig);
  }

  @Override
  void generateSpans() {
    spanQueue = spanGenerator.generate();
  }

  @Override
  void sendSpans() throws Exception {
    spanSender.startSending(spanQueue);
  }

  @Override
  void dumpStatistics() throws Exception {
    if (!Strings.isNullOrEmpty(generatorConfig.getStatisticsFile())) {
      FileWriter fileWriter = new FileWriter(new File(generatorConfig.getStatisticsFile()));
      fileWriter.write(spanGenerator.getStatistics().toJSONString());
      fileWriter.close();
    } else {
      System.out.println(spanGenerator.getStatistics());
    }
  }
}