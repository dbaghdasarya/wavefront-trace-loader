package com.wavefront.generators;

import com.wavefront.DataQueue;
import com.wavefront.config.GeneratorConfig;
import com.wavefront.datastructures.StatSpan;
import com.wavefront.datastructures.Trace;
import com.wavefront.sdk.common.Pair;

import org.apache.commons.lang3.NotImplementedException;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import static com.wavefront.helpers.Defaults.ANSI_RESET;
import static com.wavefront.helpers.Defaults.ANSI_YELLOW;
import static com.wavefront.helpers.Defaults.GIGA;

/**
 * Common interface for various trace generators.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public abstract class TraceGenerator extends BasicGenerator {
  protected GeneratorConfig generatorConfig;
  private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
  private double usedHeapMemoryGB = 0;
  private final double maxHeapMemoryGB =
      (double) memoryMXBean.getHeapMemoryUsage().getMax() / GIGA;
  private long maxDataQueueSize;
  private boolean isSleep;
  private final double MAX_ALLOWED_HEAP_MEMORY = (maxHeapMemoryGB * 90) / 100;
  private final int QUEUE_LOW_LIMIT = 20;

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
        current += GENERATION_DELAY_MILLIS;
      }
      mustBeGeneratedSpans = (rate * (current - start) / 1000);

      while (generatedSpans < mustBeGeneratedSpans) {
        final Trace trace = generateTrace(current - RANDOM.nextInt(sleeping(isRealTime)));
        if (trace != null) {
          dataQueue.addTrace(trace);
          generatedSpans += trace.getSpansCount();
        } else {
          break;
        }
        updateHeapMemory();
      }
    }
    logger.info("Generation complete!\n" + ANSI_YELLOW + String.format(generatorConfig.getGeneratorConfigFile() +
        " Memory " + "usage- %.2fGB / %.2fGB", usedHeapMemoryGB, maxHeapMemoryGB) + ANSI_RESET);
    sendStat(str);
  }

  private int sleeping(boolean isRealTime) {
    if (isRealTime) {
      long start = System.currentTimeMillis();
      while (isSleep) {
        if (dataQueue.size() <= (maxDataQueueSize * QUEUE_LOW_LIMIT) / 100) {
          isSleep = false;
        }
        try {
          Thread.sleep(SLEEP_DELAY_MILLISECONDS);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      long end = Math.max(System.currentTimeMillis(), start + 1);
      return (int) (end - start);
    } else {
      return GENERATION_DELAY_MILLIS;
    }
  }

  private void updateHeapMemory() {
    double currentUsed = (double) memoryMXBean.getHeapMemoryUsage().getUsed() / GIGA;
    if (usedHeapMemoryGB < currentUsed) {
      usedHeapMemoryGB = currentUsed;
    }
    if (currentUsed >= MAX_ALLOWED_HEAP_MEMORY) {
      maxDataQueueSize = dataQueue.size();
      isSleep = true;
    }
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

  private void sendStat(String str) {
    Trace statTrace = new Trace(2, UUID.randomUUID());

    List<Pair<String, String>> rootTags = new LinkedList<>();
    rootTags.add(new Pair<>("application", "Statistics"));
    rootTags.add(new Pair<>("service", "Statistics"));

    rootTags.add(new Pair<>("Total traces", Integer.toString(statistics.getTracesSum())));
    rootTags.add(new Pair<>("Total errors", Integer.toString(statistics.getErrorsSum())));
    rootTags.add(new Pair<>("Total errors percentage", Long.toString(Math.round((double) statistics.getErrorsSum() / statistics.getTracesSum() * 100))));
    rootTags.add(new Pair<>("Total debug spans", Integer.toString(statistics.getDebugSpansSum())));

    String rootName = str + "_STAT";

    StatSpan stat = new StatSpan(rootName,
        System.currentTimeMillis(),
        1,
        "traceLoaderHost",
        statTrace.getTraceUUID(),
        UUID.randomUUID(),
        null,
        null, rootTags,
        null);

    statTrace.add(0, stat);

    statistics.getTracesByType().forEach((k, v) -> {
      List<UUID> parents_list = new LinkedList();
      parents_list.add(stat.getSpanUUID());

      List<Pair<String, String>> traceTypeTags = new LinkedList<>();
      traceTypeTags.add(new Pair<>("application", "Statistics"));
      traceTypeTags.add(new Pair<>("service", "TraceType"));
      traceTypeTags.add(new Pair<>("Count", Integer.toString(v.getCount())));
      traceTypeTags.add(new Pair<>("Percentage", Double.toString(100.0 * v.getCount() / getStatistics().getTracesSum())));
      traceTypeTags.add(new Pair<>("Spans mean", Long.toString(Math.round((double) v.getSpansSum() / v.getCount()))));
      traceTypeTags.add(new Pair<>("Spans min", Integer.toString(v.getSpansMin())));
      traceTypeTags.add(new Pair<>("Spans max", Integer.toString(v.getSpansMax())));
      traceTypeTags.add(new Pair<>("Trace duration mean", Long.toString(Math.round((double) v.getTraceDuration() / v.getCount()))));
      traceTypeTags.add(new Pair<>("Trace duration min", Long.toString(v.getTraceDurationMin())));
      traceTypeTags.add(new Pair<>("Trace duration max", Long.toString(v.getTraceDurationMax())));
      traceTypeTags.add(new Pair<>("Errors count", Integer.toString(v.getErrorCount())));
      traceTypeTags.add(new Pair<>("Errors percentage", Long.toString(Math.round((double) v.getErrorCount() / getStatistics().getErrorsSum() * 100))));
      traceTypeTags.add(new Pair<>("Debug spans count", Integer.toString(v.getDebugSpansCount())));

      StatSpan typeStat = new StatSpan(k,
          System.currentTimeMillis(),
          1,
          "localhost",
          statTrace.getTraceUUID(),
          UUID.randomUUID(),
          parents_list,
          null, traceTypeTags,
          null);
      statTrace.add(1, typeStat);
      statTrace.setRoot(rootName);
      dataQueue.addTrace(statTrace);
    });
  }
}