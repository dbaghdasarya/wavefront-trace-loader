package com.wavefront;

import com.wavefront.sdk.common.WavefrontSender;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * TODO
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public class SpanSender {
  private static final Logger logger = Logger.getLogger(SpanSender.class.getCanonicalName());
  private static LoadingCache<String, FileWriter> fileWriterLoadingCache = CacheBuilder.newBuilder()
      .build(new CacheLoader<>() {
        @Override
        public FileWriter load(String fileName) throws Exception {
          return new FileWriter(new File(fileName));
        }
      });

  private FileWriter fileWriter;

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
      System.out.println("mustBeSent = " + mustBeSent + ", sent = " + sent);
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
        System.out.println("timestamp - " + tempSpan.getStartMillis());
        sent++;
      }

      TimeUnit.MILLISECONDS.sleep(50);
    }

    logger.info("Sending complete!");
  }

  private void saveToFile(SpanQueue spanQueue) throws ExecutionException, IOException {
    fileWriter = fileWriterLoadingCache.get(outputFile);
    int sent = 0;
    Span tempSpan;
    while ((tempSpan = spanQueue.pollFirst()) != null) {
      fileWriter.write(tempSpan.toString());
      sent++;
    }
    logger.info(sent + " spans saved to file!");
  }
}
