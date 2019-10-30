package com.wavefront;

import com.wavefront.sdk.common.WavefrontSender;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * TODO
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public class SpanSender {
  private static final Logger logger = Logger.getLogger(SpanSender.class.getCanonicalName());
  private WavefrontSender spanSender;


  public SpanSender(WavefrontSender wavefrontSender) {
    spanSender = wavefrontSender;
  }

  public void startSending(double rate, SpanQueue spanQueue) throws IOException, InterruptedException {
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
}
