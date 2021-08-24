package com.wavefront;

import com.google.common.base.Strings;

import com.wavefront.config.ApplicationConfig;
import com.wavefront.generators.BasicGenerator;
import com.wavefront.generators.ReIngestGenerator;
import com.wavefront.internal.reporter.WavefrontInternalReporter;
import com.wavefront.opentracing.reporting.WavefrontSpanReporter;
import com.wavefront.sdk.common.WavefrontSender;
import com.wavefront.sdk.common.clients.WavefrontClientFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Input point of application to generate and send traces to wavefront.
 *
 * @author Sirak Ghazaryan (sghazaryan@vmware.com)
 */
public class WavefrontTraceLoader extends AbstractTraceLoader {
  private DataQueue dataQueue;
  private BasicGenerator basicGenerator;
  private SpanSender spanSender;

  WavefrontTraceLoader(ApplicationConfig applicationConfig) {
    this.applicationConfig = applicationConfig;
  }

  WavefrontTraceLoader() {
  }

  public static void main(String[] args) throws IOException {
    new WavefrontTraceLoader().start(args);
  }

  @Override
  void setupSenders() throws IOException {
    if (applicationConfig == null) {
      throw new IOException("Application config should contain proxy or direction ingestion info.");
    }
    // Spans or Traces should be exported to file.
    if (!Strings.isNullOrEmpty(applicationConfig.getSpanOutputFile())
        || !Strings.isNullOrEmpty(applicationConfig.getTraceOutputFile())) {
      spanSender = new SpanSender(applicationConfig.getSpanOutputFile(),
          applicationConfig.getTraceOutputFile(), dataQueue);
    } else {
      WavefrontSender wfSender;
      WavefrontClientFactory wfClientFactory = new WavefrontClientFactory();
      if (applicationConfig.getProxyServer() != null) {
        wfClientFactory.addClient(applicationConfig.getProxyServer() + ":" +
            applicationConfig.getMetricsPort() + "/");
        wfClientFactory.addClient(applicationConfig.getProxyServer() + ":" +
            applicationConfig.getDistributionPort() + "/");
        wfClientFactory.addClient(applicationConfig.getProxyServer() + ":" +
            applicationConfig.getTracingPort() + "/");
        wfClientFactory.addClient(applicationConfig.getProxyServer() + ":" +
            applicationConfig.getCustomTracingPorts() + "/");
      } else {
        wfClientFactory.addClient("https://" + applicationConfig.getToken() +
            "@" + applicationConfig.getServer());
      }
      wfSender = wfClientFactory.getClient();
      WavefrontSpanReporter wfSpanReporter = new WavefrontSpanReporter.Builder().build(wfSender);
      wfSpanReporter.setMetricsReporter(new WavefrontInternalReporter.Builder().build(wfSender));
      spanSender = new SpanSender(wfSender, generatorConfig.getSpansRate(), dataQueue);
    }
  }

  @Override
  void initialize() {
    dataQueue = new DataQueue(!Strings.isNullOrEmpty(applicationConfig.getTraceOutputFile()));
  }

  @Override
  void startLoading() throws Exception {
    if (Strings.isNullOrEmpty(applicationConfig.getSpanOutputFile())
        && Strings.isNullOrEmpty(applicationConfig.getTraceOutputFile())) {
      // Generate and send spans at the same time
      realTimeSending();
    } else {
      // Saving generated spans to file.
      basicGenerator.generateForFile();
      spanSender.saveToFile();
    }
  }

  @Override
  void setupGenerators() {
    if (applicationConfig != null && !Strings.isNullOrEmpty(applicationConfig.getWfTracesFile())) {
      this.basicGenerator = new ReIngestGenerator(applicationConfig.getWfTracesFile(), dataQueue);
    } else {
      this.basicGenerator = generatorConfig.getGenerator(dataQueue);
    }
  }

  @Override
  void dumpStatistics() throws Exception {
    if (!Strings.isNullOrEmpty(generatorConfig.getStatisticsFile())) {
      FileWriter fileWriter = new FileWriter(new File(generatorConfig.getStatisticsFile()));
      fileWriter.write(basicGenerator.getStatistics().toJSONString());
      fileWriter.close();
    } else {
      LOGGER.info(basicGenerator.getStatistics().toString());
    }
  }

  void realTimeSending() throws InterruptedException {
    // Send spans to host.
    Thread generator = new Thread(basicGenerator);
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