package com.wavefront;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;

import com.wavefront.TraceTypePattern.Distribution;
import com.wavefront.config.GeneratorConfig;
import com.wavefront.helpers.Defaults;
import com.wavefront.helpers.Statistics;
import com.wavefront.sdk.common.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

/**
 * Class generates traces based on {@link GeneratorConfig} parameters.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public class SpanGenerator implements Runnable {
  private static final Logger LOGGER = Logger.getLogger(SpanSender.class.getCanonicalName());
  private static final Random RANDOM = new Random(System.currentTimeMillis());
  private static final int HUNDRED_PERCENT = 100;
  private final Statistics statistics = new Statistics();
  private final GeneratorConfig generatorConfig;
  private final SpanQueue spanQueue;

  private final LoadingCache<TraceTypePattern, List<Integer>> spansDistributionsPercentages =
      CacheBuilder.newBuilder().expireAfterAccess(2, TimeUnit.MINUTES).
          build(new CacheLoader<>() {
            @Override
            public List<Integer> load(@Nonnull TraceTypePattern traceTypePattern) {
              return traceTypePattern.spansDistributions.stream().map(distribution ->
                  distribution.percentage).collect(Collectors.toList());
            }
          });
  private final LoadingCache<TraceTypePattern, List<Integer>> traceDurationsPercentages =
      CacheBuilder.newBuilder().expireAfterAccess(2, TimeUnit.MINUTES).
          build(new CacheLoader<>() {
            @Override
            public List<Integer> load(@Nonnull TraceTypePattern traceTypePattern) {
              return traceTypePattern.traceDurations.stream().map(distribution ->
                  distribution.percentage).collect(Collectors.toList());
            }
          });
  private final LoadingCache<TraceTypePattern, List<Integer>> spansDurationsPercentages =
      CacheBuilder.newBuilder().expireAfterAccess(2, TimeUnit.MINUTES).
          build(new CacheLoader<>() {
            @Override
            public List<Integer> load(@Nonnull TraceTypePattern traceTypePattern) {
              return traceTypePattern.spansDurations.stream().map(distribution ->
                  distribution.percentage).collect(Collectors.toList());
            }
          });
  private List<Integer> tracePercentages;

  public SpanGenerator(GeneratorConfig config, SpanQueue spanQueue) {
    this.generatorConfig = config;
    this.spanQueue = spanQueue;
  }

  public void generate() {
    // normalize percentages of distribution to fix wrong inputs
    normalizeDistributions(generatorConfig.getTraceTypePatterns());
    tracePercentages = generatorConfig.getTraceTypePatterns().stream().
        map(traceTypePattern -> traceTypePattern.tracePercentage).collect(Collectors.toList());

    Function<SpanQueue, Boolean> whileCheck;
    int traceCount = generatorConfig.getTotalTraceCount();
    int spansCount;
    if (traceCount > 0) {
      LOGGER.info("Should be generated " + traceCount + " traces.");
      whileCheck = queue -> queue.getEnteredTraceCount() < traceCount;
    } else {
      spansCount = generatorConfig.getSpansRate() * (int) generatorConfig.getDuration().toSeconds();
      LOGGER.info("Should be generated " + spansCount + " spans.");
      whileCheck = queue -> queue.getEnteredTraceCount() < spansCount;
    }

    while (whileCheck.apply(spanQueue)) {
      // get next trace type to be generated
      TraceTypePattern traceTypePattern = getNextTraceType(generatorConfig.getTraceTypePatterns());

      spanQueue.addTrace(generateTrace(traceTypePattern));
    }
    LOGGER.info("Generation complete!");
  }

  /**
   * Returns statistics about the generated traces.
   */
  public Statistics getStatistics() {
    return statistics;
  }

  /**
   * Generate trace for a given trace type.
   */
  private List<List<Span>> generateTrace(@Nonnull TraceTypePattern traceType) {
    int levels = traceType.nestingLevel;
    int spanNumbers = getNextSpanDistribution(traceType).getValue() - 1;
    int spanDuration;
    int lastSpanDuration = 0;
    boolean useSpansDistribution = false;
    int traceDuration = 0;
    boolean error = false;

    // traceDurations has priority,so if it is set spansDurations is skipped
    if (!traceType.traceDurations.isEmpty()) {
      traceDuration = getNextTraceDuration(traceType).getValue();
      // currently all spans have the same duration, expect last one to ensure expected duration
      spanDuration = spanNumbers == 0 ? traceDuration : traceDuration / spanNumbers;
      lastSpanDuration = spanNumbers == 0 ? 0 : traceDuration % spanNumbers;
    } else {
      useSpansDistribution = true;
      spanDuration = getNextSpanDuration(traceType).getValue();
      traceDuration += spanDuration;
    }

    long currentTime = System.currentTimeMillis();
    String suffixes = "abcdefghijklmnopqrstuvxyz";
    int sufLen = suffixes.length();

    List<List<Span>> trace = Lists.newArrayListWithExpectedSize(levels);
    for (int n = 0; n < levels; n++) {
      trace.add(new LinkedList<>());
    }

    // Head span
    UUID traceUUID = UUID.randomUUID();
    trace.get(0).add(new Span(
        traceType.traceTypeName,
        currentTime,
        spanDuration,
        "localhost", // + suffixes.charAt(rand.nextInt(sufLen)),
        traceUUID,
        UUID.randomUUID(),
        null,
        null,
        getTags(traceType, traceType.errorRate),
        null));


    while (spanNumbers > 0) {
      for (int n = 1; n < levels && spanNumbers > 0; n++) {
        for (int m = n; m < levels && spanNumbers > 0; m++) {
          currentTime += spanDuration;

          if (useSpansDistribution) {
            spanDuration = getNextSpanDuration(traceType).getValue();
            traceDuration += spanDuration;
          } else if (spanNumbers == 1) {
            // in case of traceDuration is usedthe remaining part of duration will be added to the
            // last span
            spanDuration = lastSpanDuration;
          }
          trace.get(m).add(new Span(
              "name_" + suffixes.charAt(RANDOM.nextInt(sufLen)),
              currentTime,
              spanDuration,
              "localhost", // + suffixes.charAt(rand.nextInt(sufLen)),
              traceUUID,
              UUID.randomUUID(),
              null,
              null,
              // Not root spans will have error tag if ErrorConditions defined
              getTags(traceType, 0),
              null));
          spanNumbers--;
        }
      }
    }

    int upperLevelSize;
    for (int n = levels - 1; n > 0; n--) {
      upperLevelSize = trace.get(n - 1).size();
      for (Span childSpan : trace.get(n)) {
        childSpan.addParent(trace.get(n - 1).get(RANDOM.nextInt(upperLevelSize)));
      }
    }
    statistics.offer(traceType.traceTypeName, trace, traceDuration);
    return trace;
  }

  private List<Pair<String, String>> getTags(TraceTypePattern pattern, int errorRate) {
    List<Pair<String, String>> tags = new LinkedList<>();

    // add all mandatory tags
    pattern.mandatoryTags.forEach(tag ->
        tags.add(new Pair<>(tag.tagName, tag.tagValues.get(RANDOM.nextInt(tag.tagValues.size())))));

    // add some of optional tags if exist
    if (pattern.optionalTagsPercentage > 0 && pattern.optionalTags != null) {
      pattern.optionalTags.forEach(tag -> {
        if (RANDOM.nextInt(HUNDRED_PERCENT) + 1 <= pattern.optionalTagsPercentage) {
          tags.add(new Pair<>(tag.tagName, tag.tagValues.get(RANDOM.nextInt(tag.tagValues.size()))));
        }
      });
    }

    // Check error conditions.
    // If user sets errorConditions for the trace type it will override
    // the common errorRate. Otherwise the common errorRate will be applied to the first span.
    if (pattern.errorConditions != null) {
      errorRate = 0;
      for (TraceTypePattern.ErrorCondition condition : pattern.errorConditions) {
        if (tags.stream().
            anyMatch(tag -> tag._1.equals(condition.tagName) && tag._2.equals(condition.tagValue))) {
          // the effective Error Rate will be treated as a summary of probability of independent
          // events P(AB) = P(A) + P(B) - P(A) * P(B)
          errorRate += condition.errorRate - errorRate * condition.errorRate / HUNDRED_PERCENT;
          if (errorRate > HUNDRED_PERCENT) {
            break;
          }
        }
      }
    }

    if (errorRate > 0) {
      if (RANDOM.nextInt(HUNDRED_PERCENT) < errorRate) {
        tags.add(new Pair<>("error", "true"));
      }
    }
    if (pattern.debugRate > 0) {
      if (RANDOM.nextInt(HUNDRED_PERCENT) < pattern.debugRate) {
        tags.add(new Pair<>("debug", "true"));
      }
    }

    return tags;
  }

  /**
   * Normalize all distributions. ie trace type, spans counts and so on.
   */
  private void normalizeDistributions(@Nonnull List<TraceTypePattern> traceTypePatterns) {
    // trace types and spans count distribution
    double tracePercRatio = getNormalizationRatio(traceTypePatterns.stream().
        mapToDouble(t -> t.tracePercentage).sum());

    traceTypePatterns.forEach(traceType -> {
      traceType.tracePercentage = (int) Math.round(traceType.tracePercentage * tracePercRatio);
      normalizeCanonicalDistributions(traceType.spansDistributions);
      normalizeCanonicalDistributions(traceType.traceDurations);
      normalizeCanonicalDistributions(traceType.spansDurations);
    });
  }

  /**
   * Normalize distributions which are inherited from Distribution class.
   *
   * @param distributions Distributions to be normalized.
   */
  private void normalizeCanonicalDistributions(List<Distribution> distributions) {
    double ratio = getNormalizationRatio(distributions.stream().
        mapToDouble(d -> d.percentage).sum());
    // don't do anything if input values are already normalized
    if (Double.compare(ratio, 1) != 0) {
      distributions.forEach(d ->
          d.percentage = (int) Math.round(d.percentage * ratio));
    }
  }

  /**
   * Get normalization ratio for given sum of percentages.
   */
  private double getNormalizationRatio(double percentsSum) {
    double ratio = 1;
    if (Double.compare(percentsSum, HUNDRED_PERCENT) != 0) {
      LOGGER.warning("Distributions summary percentage must be 100. " +
          "Normalizing in range 0-100");
      ratio = (double) HUNDRED_PERCENT / percentsSum;
    }
    return ratio;
  }

  /**
   * Get next span distribution for the given trace type.
   */
  private Distribution getNextSpanDistribution(@Nonnull TraceTypePattern traceTypePattern) {
    return traceTypePattern.spansDistributions.get(
        getIndexOfNextItem(spansDistributionsPercentages.getUnchecked(traceTypePattern)));
  }

  /**
   * Get next span duration for the given trace type.
   */
  private Distribution getNextSpanDuration(@Nonnull TraceTypePattern traceTypePattern) {
    return traceTypePattern.spansDurations.get(
        getIndexOfNextItem(spansDurationsPercentages.getUnchecked(traceTypePattern)));
  }

  /**
   * Get nex trace type for generation based on trace type distributions.
   *
   * @param traceTypePatterns List of trace types to be generated.
   */
  private TraceTypePattern getNextTraceType(@Nonnull List<TraceTypePattern> traceTypePatterns) {
    return traceTypePatterns.get(getIndexOfNextItem(tracePercentages));
  }

  /**
   * Get next trace duration distribution of the given trace type.
   */
  private Distribution getNextTraceDuration(@Nonnull TraceTypePattern traceTypePattern) {
    return traceTypePattern.traceDurations.get(
        getIndexOfNextItem(traceDurationsPercentages.getUnchecked(traceTypePattern)));
  }

  /**
   * Generic helper method which calculate next item based on distribution percentages using
   * randomization.
   *
   * @param percentages List of distribution percentages.
   * @return Return an index of the next item.
   */
  private int getIndexOfNextItem(@Nonnull List<Integer> percentages) {
    int randomPercent = RANDOM.nextInt(HUNDRED_PERCENT) + 1;
    int left = 0;
    for (int i = 0; i < percentages.size(); i++) {
      if (randomPercent > left && randomPercent <= percentages.get(i) + left) {
        return i;
      } else {
        left += percentages.get(i);
      }
    }
    LOGGER.warning("Next item index doesn't matched. Return first item index.");
    return 0;
  }

  @Override
  public void run() {
    LOGGER.info("Generating spans ...");
    // normalize percentages of distribution to fix wrong inputs
    normalizeDistributions(generatorConfig.getTraceTypePatterns());
    tracePercentages = generatorConfig.getTraceTypePatterns().stream().
        map(traceTypePattern -> traceTypePattern.tracePercentage).collect(Collectors.toList());

    Function<SpanQueue, Boolean> whileCheck;
    int traceCount = generatorConfig.getTotalTraceCount();
    int spansCount;
    int rate = generatorConfig.getSpansRate();
    if (traceCount > 0) {
      LOGGER.info("Should be generated " + traceCount + " traces.");
      whileCheck = queue -> queue.getEnteredTraceCount() < traceCount;
    } else {
      spansCount = rate * (int) generatorConfig.getDuration().toSeconds();
      LOGGER.info("Should be generated " + spansCount + " spans.");
      whileCheck = queue -> queue.getEnteredSpanCount() < spansCount;
    }

    long start = System.currentTimeMillis();
    long current;
    int mustBeGeneratedSpans;
    int generatedSpans = 0;
    long sleepMillis = Math.max(Defaults.MIN_SLEEP_MILLIS, 1000 / generatorConfig.getSpansRate());
    while (whileCheck.apply(spanQueue)) {
      current = System.currentTimeMillis();
      mustBeGeneratedSpans = (int) (rate * (current - start) / 1000);

      while (generatedSpans < mustBeGeneratedSpans) {
        // get next trace type to be generated
        TraceTypePattern traceTypePattern = getNextTraceType(generatorConfig.getTraceTypePatterns());
        List<List<Span>> trace = generateTrace(traceTypePattern);
        spanQueue.addTrace(trace);
        for (List<Span> spanList :
            trace) {
          generatedSpans += spanList.size();
        }
      }

      try {
        Thread.sleep(sleepMillis);
      } catch (InterruptedException e) {
        LOGGER.severe(Throwables.getStackTraceAsString(e));
      }
    }
    LOGGER.info("Generation complete!");
  }
}
