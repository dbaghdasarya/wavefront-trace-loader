package com.wavefront;

import com.google.common.base.Strings;

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
  private final SpanQueue spanQueue = new SpanQueue();
  private SpanGenerator spanGenerator;
  private SpanSender spanSender;

  public static void main(String[] args) throws IOException {
    new WavefrontTraceLoader().start(args);
  }

  @Override
  void setupSenders() throws IOException {
    if (applicationConfig == null) {
      throw new IOException("Application config should contain proxy or direction ingestion info.");
    }
    // TODO do we need additional checks here??
    if (!Strings.isNullOrEmpty(applicationConfig.getOutputFile())) {
      spanSender = new SpanSender(applicationConfig.getOutputFile(), spanQueue);
    } else {
      WavefrontSender wavefrontSender;
      if (applicationConfig.getProxyServer() != null) {
        wavefrontSender = new WavefrontProxyClient.Builder(applicationConfig.getProxyServer()).
            metricsPort(applicationConfig.getMetricsPort()).
            distributionPort(applicationConfig.getDistributionPort()).
            tracingPort(applicationConfig.getTracingPort()).build();
      } else {
        wavefrontSender = new WavefrontDirectIngestionClient.Builder(applicationConfig.getServer(),
            applicationConfig.getToken()).build();
      }

      spanSender = new SpanSender(wavefrontSender, generatorConfig.getSpansRate(), spanQueue);
    }
  }

  @Override
  void startLoading() throws Exception {
    if (Strings.isNullOrEmpty(applicationConfig.getOutputFile())) {
      // Generate and send spans at the same time
      realTimeSending();
    } else {
      // Saving generated spans to file.
      spanGenerator.generateForFile();
      spanSender.saveToFile();
    }
  }

  @Override
  void setupGenerators() {
    this.spanGenerator = new SpanGenerator(generatorConfig, spanQueue);
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

  void realTimeSending() throws InterruptedException {
    // Send spans to host.
    Thread generator = new Thread(spanGenerator);
    Thread sender = new Thread(spanSender);
    generator.start();
    sender.start();
    // Waiting while generation completes.
    generator.join();
    // Inform sender that generation completes.
    spanSender.stopSending();
    // Wait while sender devastates the span queue.
    sender.join();
  }
}