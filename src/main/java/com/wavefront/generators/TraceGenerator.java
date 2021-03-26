package com.wavefront.generators;

import com.google.common.base.Throwables;

import com.wavefront.DataQueue;
import com.wavefront.config.GeneratorConfig;
import com.wavefront.datastructures.Trace;

import org.apache.commons.lang3.NotImplementedException;

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
                                 @Nonnull Logger logger) {
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