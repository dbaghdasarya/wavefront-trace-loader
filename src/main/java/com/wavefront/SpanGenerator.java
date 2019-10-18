package com.wavefront;

import com.wavefront.config.GeneratorConfig;
import com.wavefront.sdk.common.Pair;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public class SpanGenerator {
  private static final Logger logger = Logger.getLogger(SpanSender.class.getCanonicalName());

  public SpanQueue generate(GeneratorConfig config) {
    SpanQueue spanQueue = new SpanQueue();
    long spansNumber = (long)(config.getSpansRate() * config.getDuration().toSeconds());
    long spanDuration = (long)(1000.0 / config.getSpansRate());
    logger.log( Level.INFO, "Should be genereted " + spansNumber + " spans, with spanDuration - " +
            spanDuration);

    LinkedList<TraceTypePattern> traceTypes = config.getTraceTypes();
    for (TraceTypePattern traceTypePattern: traceTypes) {
      for(int n = 0; n < traceTypePattern.tracesNumber;n++){
        spanQueue.addTrace(treceGenerator( traceTypePattern, config.getSpansRate()));
      }
    }

    logger.log(Level.INFO, "Generation complete!");
    return spanQueue;
  }


  protected LinkedList<Span>[] treceGenerator( TraceTypePattern traceTypePattern, double rate){
    int levels = traceTypePattern.nestingLevel;
    int spanNumbers = traceTypePattern.spansNumber;
    spanNumbers--;

    long spanDuration = (long)(1000.0 / rate);
    long startTime = Calendar.getInstance().getTimeInMillis();
    long currentTime = startTime;
    Random rand = new Random();
    String suffixes = "abcdefg";
    int sufLen = suffixes.length();


    List<Pair<String, String>> tags = new LinkedList<Pair<String, String>>();
    tags.add(new Pair<String, String>("application", "trace loader"));
    tags.add(new Pair<String, String>("service", "generator"));
    tags.add(new Pair<String, String>("host", "ip-10.20.30.40"));

    LinkedList<Span>[] trace = new LinkedList[levels];
    for(int n = 0; n < levels; n++){
      trace[n] = new LinkedList<>();
    }

    // Head span
    UUID traceUUID = UUID.randomUUID();
    trace[0].add(new Span(
            traceTypePattern.traceTypeName,
            currentTime,
            spanDuration,
            "localhost", // + suffixes.charAt(rand.nextInt(sufLen)),
            traceUUID,
            UUID.randomUUID(),
            null,
            null,
            tags,
            null));

    while (spanNumbers > 0){
      for( int n = 1; n < levels && spanNumbers > 0; n++){
        for(int m = n; m < levels && spanNumbers > 0; m++){
          trace[m].add(new Span(
                  "name_" + suffixes.charAt(rand.nextInt(sufLen)),
                  currentTime,
                  spanDuration,
                  "localhost", // + suffixes.charAt(rand.nextInt(sufLen)),
                  traceUUID,
                  UUID.randomUUID(),
                  null,
                  null,
                  tags,
                  null));
          spanNumbers--;
          currentTime += spanDuration;
        }
      }
    }

    int upperLevelSize;
    for (int n = levels - 1; n > 0; n--){
      upperLevelSize = trace[n-1].size();
      for (Span childSpan:
              trace[n]) {
        childSpan.addParent(trace[n-1].get(rand.nextInt(upperLevelSize)));
      }
    }
    return trace;
  }
}
