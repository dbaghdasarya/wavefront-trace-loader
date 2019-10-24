package com.wavefront;

import com.google.common.collect.Lists;

import com.wavefront.config.GeneratorConfig;
import com.wavefront.sdk.common.Pair;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public class SpanGenerator {
  private static final Logger logger = Logger.getLogger(SpanSender.class.getCanonicalName());
  private static final Random randGenerator = new Random();

  public SpanQueue generate(GeneratorConfig config) {
    final SpanQueue spanQueue = new SpanQueue();
    long spansNumber = (long) (config.getTracesRate() * config.getDuration().toSeconds());
    long spanDuration = (long) (1000.0 / config.getTracesRate());
    logger.log(Level.INFO, "Should be genereted " + spansNumber + " spans, with spanDuration - " +
        spanDuration);

    config.getTraceTypes().forEach(traceTypePattern -> {
      for (int n = 0; n < traceTypePattern.tracesNumber; n++) {
        spanQueue.addTrace(traceGenerator(traceTypePattern, config.getTracesRate()));
      }
    });

    logger.log(Level.INFO, "Generation complete!");
    return spanQueue;
  }


  protected List<List<Span>> traceGenerator(TraceTypePattern traceTypePattern,
                                            double rate) {
    int levels = traceTypePattern.nestingLevel;
    int spanNumbers = traceTypePattern.spansNumber;
    spanNumbers--;

    long spanDuration = (long) (1000.0 / rate);
    long startTime = Calendar.getInstance().getTimeInMillis();
    long currentTime = startTime;
    String suffixes = "abcdefg";
    int sufLen = suffixes.length();

    List<List<Span>> trace = Lists.newArrayListWithExpectedSize(levels);
    for (int n = 0; n < levels; n++) {
      trace.add(new LinkedList<>());
    }

    // Head span
    UUID traceUUID = UUID.randomUUID();
    trace.get(0).add(new Span(
        traceTypePattern.traceTypeName,
        currentTime,
        spanDuration,
        "localhost", // + suffixes.charAt(rand.nextInt(sufLen)),
        traceUUID,
        UUID.randomUUID(),
        null,
        null,
        getTags(traceTypePattern.errorRate),
        null));

    while (spanNumbers > 0) {
      for (int n = 1; n < levels && spanNumbers > 0; n++) {
        for (int m = n; m < levels && spanNumbers > 0; m++) {
          trace.get(m).add(new Span(
              "name_" + suffixes.charAt(randGenerator.nextInt(sufLen)),
              currentTime,
              spanDuration,
              "localhost", // + suffixes.charAt(rand.nextInt(sufLen)),
              traceUUID,
              UUID.randomUUID(),
              null,
              null,
              getTags(0), // Errors only in the first span
              null));
          spanNumbers--;
          currentTime += spanDuration;
        }
      }
    }

    int upperLevelSize;
    for (int n = levels - 1; n > 0; n--) {
      upperLevelSize = trace.get(n - 1).size();
      for (Span childSpan :
          trace.get(n)) {
        childSpan.addParent(trace.get(n - 1).get(randGenerator.nextInt(upperLevelSize)));
      }
    }
    return trace;
  }

  protected List<Pair<String, String>> getTags(int errorRate) {
    // TODO: now it looks static, but for RCA we will generate tags variation for spans

    List<Pair<String, String>> tags = new LinkedList<>();
    tags.add(new Pair<>("application", "trace loader"));
    tags.add(new Pair<>("service", "generator"));
    tags.add(new Pair<>("host", "ip-10.20.30.40"));

    if (errorRate > 0) {
      if (randGenerator.nextInt(100) < errorRate) {
        tags.add(new Pair<>("error", "true"));
      }
    }

    return tags;
  }
}
