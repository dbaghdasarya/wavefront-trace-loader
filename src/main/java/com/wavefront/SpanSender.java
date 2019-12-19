package com.wavefront;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;

import com.wavefront.sdk.common.WavefrontSender;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * TODO
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public class SpanSender implements Runnable {
  private static final Logger LOGGER = Logger.getLogger(SpanSender.class.getCanonicalName());
  private final WavefrontSender spanSender;
  private final Integer rate;
  private final String outputFile;
  private final SpanQueue spanQueue;
  private final AtomicBoolean stopSending = new AtomicBoolean(false);


  public SpanSender(WavefrontSender wavefrontSender, Integer rate, SpanQueue spanQueue) {
    this.spanSender = wavefrontSender;
    this.rate = rate;
    this.spanQueue = spanQueue;
    this.outputFile = null;
  }

  public SpanSender(String outputFile, SpanQueue spanQueue) {
    this.spanSender = null;
    this.rate = null;
    this.spanQueue = spanQueue;
    this.outputFile = outputFile;
  }

  public void saveToFile() throws Exception {
    if (Strings.isNullOrEmpty(outputFile)) {
      LOGGER.severe("For saving spans to file, the output file name must be provided!");
      return;
    }

    final File file = new File(outputFile);
    final FileWriter fileWriter = new FileWriter(file);
    int spansCount = spanQueue.size();
    Span tempSpan;
    while ((tempSpan = spanQueue.pollFirst()) != null) {
      fileWriter.write(tempSpan.toString());
    }
    LOGGER.info(spansCount + " spans saved to file  " + file.getAbsolutePath());
  }

  @Override
  public void run() {
    LOGGER.info("Sending spans ...");
    if (rate == null || spanSender == null) {
      LOGGER.severe("SpanSender doesn't completely initialized!");
      return;
    }

    long start = System.currentTimeMillis();
    long current;
    int mustBeSentSpans;
    int sentSpans = 0;
    List<Span> spansToSend = new LinkedList<>();
    try {
      while (!stopSending.get() || spansToSend.size() > 0) {
        spansToSend.addAll(spanQueue.getReadySpans());
        current = System.currentTimeMillis();
        mustBeSentSpans = (int) (rate * (current - start) / 1000);

        ListIterator<Span> iter = spansToSend.listIterator();
        while (iter.hasNext() && sentSpans < mustBeSentSpans) {
          Span tempSpan = iter.next();
          if (tempSpan.getStartMillis() < System.currentTimeMillis()) {
            try {
              Thread.sleep(1);
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
              sentSpans++;
              iter.remove();
            } catch (IOException e) {
              LOGGER.severe(Throwables.getStackTraceAsString(e));
            }
          }
        }
      }
    } catch (InterruptedException e) {
      LOGGER.severe(Throwables.getStackTraceAsString(e));
    }
    try {
      spanSender.close();
    } catch (IOException e) {
      LOGGER.severe(Throwables.getStackTraceAsString(e));
    }
  }

  /**
   * This method only inform the sender that generation is complete. User still have to call
   * Thread.join(), for waiting while sender completes its job.
   */
  public void stopSending() {
    stopSending.set(true);
  }
}