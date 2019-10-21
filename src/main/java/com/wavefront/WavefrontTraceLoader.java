package com.wavefront;

import com.wavefront.config.ApplicationConfig;
import com.wavefront.sdk.common.WavefrontSender;
import com.wavefront.sdk.direct.ingestion.WavefrontDirectIngestionClient;
import com.wavefront.sdk.proxy.WavefrontProxyClient;

import java.io.IOException;

/**
 * TODO
 *
 * @author Sirak Ghazaryan (sghazaryan@vmware.com)
 */
public class WavefrontTraceLoader extends AbstractTraceLoader {
  private final SpanGenerator spanGenerator = new SpanGenerator();
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
    WavefrontSender wavefrontSender;
    if (config.getProxyServer() != null) {
      wavefrontSender = new WavefrontProxyClient.Builder(config.getProxyServer()).
          metricsPort(config.getMetricsPort()).
          distributionPort(config.getDistributionPort()).
          tracingPort(config.getTracingPort()).build();
    } else {
      wavefrontSender = new WavefrontDirectIngestionClient.Builder(config.getServer(), config.getToken()).build();
    }

    spanSender = new SpanSender(wavefrontSender);
  }

  @Override
  void generateSpans() {
    spanQueue = spanGenerator.generate(generatorConfig);
  }

  @Override
  void sendSpans() throws IOException, InterruptedException {
    spanSender.startSending(generatorConfig.getTracesRate(), spanQueue);
  }
}