package com.wavefront.generators;

import com.google.common.base.Throwables;

import com.wavefront.DataQueue;
import com.wavefront.config.GeneratorConfig;
import com.wavefront.datastructures.Span;
import com.wavefront.datastructures.Trace;
import com.wavefront.sdk.common.Pair;

import org.apache.commons.lang3.NotImplementedException;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

/**
 * Common interface for various trace generators.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public abstract class TraceGenerator extends BasicGenerator {
  protected GeneratorConfig generatorConfig;

  protected TraceGenerator(@Nonnull DataQueue dataQueue) {
    super(dataQueue);
  }

  /**
   * Initialize the generation process.
   */
  protected void initGeneration() {
    throw new NotImplementedException("Subclass must implement this " +
        "functionality");
  }

  protected Function<DataQueue, Boolean> getWhileCheck(@Nonnull GeneratorConfig generatorConfig,
                                                       @Nonnull Logger logger) {
    Function<DataQueue, Boolean> whileCheck;
    final int traceCount = generatorConfig.getTotalTraceCount();
    if (traceCount > 0) {
      logger.info("Should be generated " + traceCount + " traces.");
      whileCheck = queue -> queue.getEnteredTraceCount() < traceCount;
    } else {
      final long spansCount = generatorConfig.getSpansRate() *
          generatorConfig.getDuration().toSeconds();
      logger.info("Should be generated " + spansCount + " spans.");
      whileCheck = queue -> queue.getEnteredSpanCount() < spansCount;
    }

    return whileCheck;
  }

  protected void startGeneration(boolean isRealTime, @Nonnull GeneratorConfig generatorConfig,
                                 @Nonnull Logger logger, String str) {
    logger.info("Generating traces ...");

    initGeneration();

    final Function<DataQueue, Boolean> whileCheck = getWhileCheck(generatorConfig, logger);
    final long start = System.currentTimeMillis();
    long current = start;
    long mustBeGeneratedSpans;
    long generatedSpans = 0;
    long rate = generatorConfig.getSpansRate();
    while (whileCheck.apply(dataQueue)) {
      if (isRealTime) {
        current = System.currentTimeMillis();
      } else {
        // Spread traces across the whole period.
        // Simulate the delay.
        current += SLEEP_DELAY_MILLIS;
      }
      mustBeGeneratedSpans = (long) (rate * (current - start) / 1000);

      while (generatedSpans < mustBeGeneratedSpans) {
        final Trace trace = generateTrace(current - RANDOM.nextInt(SLEEP_DELAY_MILLIS));
        if (trace != null) {
          dataQueue.addTrace(trace);
          generatedSpans += trace.getSpansCount();
        }else {
          break;
        }
      }

      if (isRealTime) {
        try {
          Thread.sleep(SLEEP_DELAY_SECONDS);
        } catch (InterruptedException e) {
          logger.severe(Throwables.getStackTraceAsString(e));
        }
      }
    }

    Trace statTrace = new Trace(2, UUID.randomUUID());

    List<Pair<String, String>> list = new LinkedList<>();
    list.add(new Pair<>("application", "Statistics"));
    list.add(new Pair<>("service", "Statistics"));

    list.add(new Pair<>("Total traces", Integer.toString(statistics.getTracesSum())));
    list.add(new Pair<>("Total errors", Integer.toString(statistics.getErrorsSum())));
    list.add(new Pair<>("Total errors percentage", Long.toString(Math.round((double) statistics.getErrorsSum() / statistics.getTracesSum() * 100))));
    list.add(new Pair<>("Total debug spans", Integer.toString(statistics.getDebugSpansSum())));

    Span stat = new Span(str + "_STAT",
        System.currentTimeMillis(),
        1,
        "localhost",
        statTrace.getTraceUUID(),
        UUID.randomUUID(),
        null,
        null, list,
        null);

    statTrace.add(0, stat);

    statistics.getTracesByType().forEach((k, v) ->{
      List<UUID> parents_list = new LinkedList();
      parents_list.add(stat.getSpanUUID());

      List<Pair<String, String>> list1 = new LinkedList<>();
      list1.add(new Pair<>("application", "Statistics"));
      list1.add(new Pair<>("service", "TraceType"));
      list1.add(new Pair<>("Count", Integer.toString(v.getCount())));
      list1.add(new Pair<>("Percentage", Double.toString(100.0 * v.getCount() / getStatistics().getTracesSum())));
      list1.add(new Pair<>("Spans mean", Long.toString(Math.round((double) v.getSpansSum() / v.getCount()))));
      list1.add(new Pair<>("Spans min", Integer.toString(v.getSpansMin())));
      list1.add(new Pair<>("Spans max", Integer.toString(v.getSpansMax())));
      list1.add(new Pair<>("Trace duration mean", Long.toString(Math.round((double) v.getTraceDuration() / v.getCount()))));
      list1.add(new Pair<>("Trace duration min", Long.toString(v.getTraceDurationMin())));
      list1.add(new Pair<>("Trace duration max", Long.toString(v.getTraceDurationMax())));
      list1.add(new Pair<>("Errors count", Integer.toString(v.getErrorCount())));
      list1.add(new Pair<>("Errors percentage", Long.toString(Math.round((double) v.getErrorCount() / getStatistics().getErrorsSum() * 100))));
      list1.add(new Pair<>("Debug spans count", Integer.toString(v.getDebugSpansCount())));

      Span type_stat = new Span(k,
          System.currentTimeMillis(),
          1,
          "localhost",
          statTrace.getTraceUUID(),
          UUID.randomUUID(),
          parents_list,
          null, list1,
          null);
      statTrace.add(0, type_stat);
    });
    dataQueue.addTrace(statTrace);
    logger.info("Generation complete!");
  }

  /**
   * Generate trace for a given trace type.
   *
   * @param startMillis Start time of the trace (millis).
   * @return Generated trace.
   */
  protected Trace generateTrace(long startMillis) {
    throw new NotImplementedException("Subclass must implement this " +
        "functionality");
  }
}