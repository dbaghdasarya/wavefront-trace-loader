package com.wavefront.generators;

import com.google.common.base.Throwables;

import com.wavefront.DataQueue;
import com.wavefront.config.GeneratorConfig;
import com.wavefront.datastructures.Distribution;
import com.wavefront.datastructures.ExactTypeResolver;
import com.wavefront.datastructures.Trace;
import com.wavefront.datastructures.TypeResolver;

import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import static com.wavefront.datastructures.Distribution.HUNDRED_PERCENT;

/**
 * Common interface for various trace generators.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public abstract class TraceGenerator extends BasicGenerator {
  @Nonnull
  protected GeneratorConfig generatorConfig;
  protected List<Double> traceTypePortions = new ArrayList<>();
  protected TypeResolver typeResolver = new ExactTypeResolver();

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

  /**
   * Calculate corresponding portions of Distributions.
   */
  public void calculateDistributionsPortions(@Nonnull List<Distribution> distributions, int count) {
    for (Distribution d : distributions) {
      d.portion = d.percentage * count / 100;
    }
  }

  /**
   * Calculate corresponding portions of Distributions make as ranges.
   */
  public void calculateDistributionsPortions(@Nonnull List<Distribution> distributions) {
    double prevPortion = 0;
    for (Distribution d : distributions) {
      d.portion = d.percentage + prevPortion;
      prevPortion = d.portion;
    }
  }

  /**
   * Calculate corresponding portions of trace types make as ranges.
   */
  public void calculateTraceTypesPortions(@Nonnull List<Double> traceTypePortions) {
    double prevPortion = 0;
    for (int i = 0; i < traceTypePortions.size(); i++) {
      traceTypePortions.set(i, traceTypePortions.get(i) + prevPortion);
      prevPortion = traceTypePortions.get(i);
    }
  }

  /**
   * Return traces count.
   */
  public int getTraceCount(double tracePercentage, int totalTracesCount) {
    return (int) (totalTracesCount * tracePercentage / 100);
  }

  /**
   * Normalize all distribution percentages.
   */
  protected void normalizeDistributionsPercentages(List<Distribution> distributionsPercentages) {
    double percentagesSum = distributionsPercentages.stream().mapToDouble(d -> d.percentage).sum();
    // Don't do anything if input values are already normalized.
    if (Double.compare(percentagesSum, HUNDRED_PERCENT) != 0) {
      double ratio = HUNDRED_PERCENT / percentagesSum;
      distributionsPercentages.forEach(d -> d.percentage = d.percentage * ratio);
    }
  }

  /**
   * Get normalization ratio for given sum of percentages.
   */
  protected double getNormalizationRatio(double percentagesSum) {
    double ratio = 1;
    if (Double.compare(percentagesSum, HUNDRED_PERCENT) != 0) {
      ratio = HUNDRED_PERCENT / percentagesSum;
    }
    return ratio;
  }
}