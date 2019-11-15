package com.wavefront;

import com.google.common.base.Strings;

import com.wavefront.sdk.common.WavefrontSender;

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * TODO
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public class SpanSender {
  private static final Logger LOGGER = Logger.getLogger(SpanSender.class.getCanonicalName());
  private WavefrontSender spanSender;
  private final Integer rate;
  private final String outputFile;


  public SpanSender(WavefrontSender wavefrontSender, Integer rate) {
    this.spanSender = wavefrontSender;
    this.rate = rate;
    this.outputFile = null;
  }

  public SpanSender(String outputFile) {
    this.spanSender = null;
    this.rate = null;
    this.outputFile = outputFile;
  }

  public void startSending(SpanQueue spanQueue) throws Exception {
    if (!Strings.isNullOrEmpty(outputFile)) {
      saveToFile(spanQueue);
      return;
    }

    long start = System.currentTimeMillis();
    long current;
    int sent = 0;
    int mustBeSent;
    Span tempSpan;
    while (spanQueue.size() > 0) {
      current = System.currentTimeMillis();
      mustBeSent = (int) (rate * (current - start) / 1000);
      while (sent < mustBeSent && (tempSpan = spanQueue.pollFirst()) != null) {
        spanSender.sendSpan(
            tempSpan.getName(),
            tempSpan.getStartMillis(),
            tempSpan.getDuration(),
            tempSpan.getSource(),
            tempSpan.getTraceUUID(),
            tempSpan.getSpanUUID(),
            tempSpan.getParents(),
            null,
            tempSpan.getTags(),
            null);
        sent++;
      }

      TimeUnit.MILLISECONDS.sleep(50);
    }

    spanSender.close();
    LOGGER.info("Sending complete!");
  }

  private void saveToFile(SpanQueue spanQueue) throws Exception {
    final File file = new File(outputFile);
    final FileWriter fileWriter = new FileWriter(file);
    int spansCount = spanQueue.size();
    Span tempSpan;
    while ((tempSpan = spanQueue.pollFirst()) != null) {
      fileWriter.write(tempSpan.toString());
    }
    LOGGER.info(spansCount + " spans saved to file  " + file.getAbsolutePath());
  }
}