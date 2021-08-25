package com.wavefront;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wavefront.datastructures.Span;
import com.wavefront.datastructures.SpanKind;
import com.wavefront.datastructures.Trace;
import com.wavefront.helpers.Defaults;
import com.wavefront.sdk.common.WavefrontSender;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.annotation.Nullable;

/**
 * Performs spans sending and span/trace saving to file.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public class SpanSender implements Runnable {
  private static final Logger LOGGER = Logger.getLogger(SpanSender.class.getCanonicalName());
  private final WavefrontSender spanSender;
  private final WavefrontSender statSender;
  private final Integer rate;
  private final String spanOutputFile;
  private final String traceOutputFile;
  private final DataQueue dataQueue;
  private final AtomicBoolean stopSending = new AtomicBoolean(false);
  private boolean reportStat = false;


  public SpanSender(WavefrontSender wavefrontSender, WavefrontSender statSender, Integer rate,
                    DataQueue dataQueue, boolean reportStat) {
    this.spanSender = wavefrontSender;
    this.statSender = statSender;
    this.rate = rate;
    this.dataQueue = dataQueue;
    this.spanOutputFile = null;
    this.traceOutputFile = null;
    this.reportStat = reportStat;
  }

  public SpanSender(String spanOutputFile, String traceOutputFile, DataQueue dataQueue,
                    boolean reportStat) {
    this.spanSender = null;
    this.statSender = null;
    this.rate = null;
    this.dataQueue = dataQueue;
    this.spanOutputFile = spanOutputFile;
    this.traceOutputFile = traceOutputFile;
    this.reportStat = reportStat;
  }

  public void saveToFile() throws Exception {
    if (Strings.isNullOrEmpty(spanOutputFile) && Strings.isNullOrEmpty(traceOutputFile)) {
      LOGGER.severe("For saving spans or traces to file, at least one of the output files " +
          "name must be provided!");
      return;
    }

    if (!Strings.isNullOrEmpty(spanOutputFile)) {
      final File file = new File(spanOutputFile);
      final FileWriter fileWriter = new FileWriter(file);
      Span tempSpan;
      while ((tempSpan = dataQueue.pollFirstSpan()) != null) {
        fileWriter.write(tempSpan.toString());
      }
      fileWriter.close();
      LOGGER.info(dataQueue.getEnteredSpanCount() + " spans saved to file  " +
          file.getAbsolutePath());
    }

    if (!Strings.isNullOrEmpty(traceOutputFile) && dataQueue.getEnteredTraceCount() > 0) {
      final File file = new File(traceOutputFile);
      final FileWriter fileWriter = new FileWriter(file, true);
      Trace tempTrace;
      Set<String> roots = new HashSet<>();
      int errors = 0;
      int total = 0;
      while ((tempTrace = dataQueue.pollFirstTrace()) != null) {
        if (tempTrace.isError()) {
          errors++;
        }
        if(tempTrace.getSpans().get(0).get(0).getKind() == SpanKind.REGULAR || reportStat == true) {
          StringBuilder stringBuilder = new StringBuilder("[{\"root\":\"" + tempTrace.getRoot() +
              "\"}," + tempTrace.toWFTrace().toJSONString() + "]\n");
          if (tempTrace.getSpans().get(0).get(0).getKind() == SpanKind.STATISTICS) {
              stringBuilder.insert(0, ":)");
              fileWriter.write(stringBuilder.toString());
          }
          else {
              roots.add(tempTrace.getRoot());
              fileWriter.write(stringBuilder.toString());
              total++;
          }
        }
      }
      final ObjectMapper mapper = new ObjectMapper();
      fileWriter.write("\n" + mapper.writeValueAsString(roots));
      fileWriter.write("\n\nTotal traces - " + total + ": Erroneous - " + errors + "]\n\n");
      fileWriter.close();
      LOGGER.info(dataQueue.getEnteredTraceCount() + " trace saved to file  " +
          file.getAbsolutePath());
    }
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
        spansToSend.addAll(dataQueue.getReadySpans());
        current = System.currentTimeMillis();
        mustBeSentSpans = (int) (rate * (current - start) / 1000);

        ListIterator<Span> iter = spansToSend.listIterator();
        while (iter.hasNext() && sentSpans < mustBeSentSpans) {
          Span tempSpan = iter.next();
          if(tempSpan.getKind() == SpanKind.STATISTICS && reportStat ==true && statSender != null){
            statSender.sendSpan(tempSpan.getName(),
                tempSpan.getStartMillis(),
                tempSpan.getDuration(),
                tempSpan.getSource(),
                tempSpan.getTraceUUID(),
                tempSpan.getSpanUUID(),
                tempSpan.getParents(),
                null,
                tempSpan.getTags(),
                null);
          }
          else if (tempSpan.getStartMillis() < System.currentTimeMillis()) {
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
    } catch (InterruptedException | IOException e) {
      LOGGER.severe(Throwables.getStackTraceAsString(e));
    }

    try {
      System.out.println("Closing sender.");
      spanSender.flush();
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