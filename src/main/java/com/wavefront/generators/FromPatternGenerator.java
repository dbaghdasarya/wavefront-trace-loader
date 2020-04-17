package com.wavefront.generators;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import com.wavefront.DataQueue;
import com.wavefront.SpanSender;
import com.wavefront.config.GeneratorConfig;
import com.wavefront.datastructures.Distribution;
import com.wavefront.datastructures.Span;
import com.wavefront.datastructures.Trace;
import com.wavefront.datastructures.TraceTypePattern;
import com.wavefront.helpers.Statistics;
import com.wavefront.sdk.common.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import static com.wavefront.datastructures.Distribution.HUNDRED_PERCENT;
import static com.wavefront.datastructures.Distribution.getIndexOfNextItem;
import static com.wavefront.datastructures.Distribution.normalizeCanonicalDistributions;
import static com.wavefront.datastructures.ErrorCondition.getErrorRate;
import static com.wavefront.helpers.Defaults.DEBUG_TAG;
import static com.wavefront.helpers.Defaults.ERROR_TAG;
import static com.wavefront.helpers.WftlUtils.isEffectivePercentage;

/**
 * Class generates traces based on {@link GeneratorConfig} parameters.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public class FromPatternGenerator extends SpanGenerator {
  private static final Logger LOGGER = Logger.getLogger(SpanSender.class.getCanonicalName());
  private final Statistics statistics = new Statistics();
  @Nonnull
  private final GeneratorConfig generatorConfig;

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

  public FromPatternGenerator(@Nonnull GeneratorConfig config, @Nonnull DataQueue dataQueue) {
    super(dataQueue);
    this.generatorConfig = config;
  }

  @Override
  public void generateForFile() {
    startGeneration(false, generatorConfig, LOGGER);
  }

  @Override
  public void run() {
    startGeneration(true, generatorConfig, LOGGER);
  }

  @Override
  protected void initGeneration() {
    // normalize percentages of distribution to fix wrong inputs
    normalizeDistributions(generatorConfig.getTraceTypePatterns());
    tracePercentages = generatorConfig.getTraceTypePatterns().stream().
        map(traceTypePattern -> traceTypePattern.tracePercentage).collect(Collectors.toList());
  }

  @Override
  public Statistics getStatistics() {
    return statistics;
  }

  @Override
  protected Trace generateTrace(long startMillis) {
    // get next trace type to be generated
    final TraceTypePattern traceTypePattern =
        getNextTraceType(generatorConfig.getTraceTypePatterns());

    final int levels = traceTypePattern.nestingLevel;
    int spanNumbers = getNextSpanDistribution(traceTypePattern).getValue() - 1;
    int spanDuration;
    int lastSpanDuration = 0;
    boolean useSpansDistribution = false;
    int traceDuration = 0;

    // traceDurations has priority,so if it is set spansDurations is skipped
    if (!traceTypePattern.traceDurations.isEmpty()) {
      traceDuration = getNextTraceDuration(traceTypePattern).getValue();
      // currently all spans have the same duration, expect last one to ensure expected duration
      spanDuration = spanNumbers == 0 ? traceDuration : traceDuration / spanNumbers;
      lastSpanDuration = spanNumbers == 0 ? 0 : traceDuration % spanNumbers;
    } else {
      useSpansDistribution = true;
      spanDuration = getNextSpanDuration(traceTypePattern).getValue();
      traceDuration += spanDuration;
    }

    String suffixes = traceTypePattern.spanNameSuffixes;
    int sufLen = suffixes.length();

    final UUID traceUUID = UUID.randomUUID();
    final Trace trace = new Trace(levels, traceUUID);

    // Root span
    trace.add(0, new Span(
        traceTypePattern.traceTypeName,
        startMillis,
        spanDuration,
        "localhost", // + suffixes.charAt(rand.nextInt(sufLen)),
        traceUUID,
        UUID.randomUUID(),
        null,
        null,
        getTags(traceTypePattern, traceTypePattern.traceTypeName, traceTypePattern.errorRate),
        null));


    while (spanNumbers > 0) {
      for (int n = 1; n < levels && spanNumbers > 0; n++) {
        for (int m = n; m < levels && spanNumbers > 0; m++) {
          startMillis += spanDuration;

          if (useSpansDistribution) {
            spanDuration = getNextSpanDuration(traceTypePattern).getValue();
            traceDuration += spanDuration;
          } else if (spanNumbers == 1) {
            // in case of traceDuration is usedthe remaining part of duration will be added to the
            // last span
            spanDuration = lastSpanDuration;
          }
          String spanName = "name_" + suffixes.charAt(RANDOM.nextInt(sufLen));
          trace.add(m, new Span(
              spanName,
              startMillis,
              spanDuration,
              "localhost", // + suffixes.charAt(rand.nextInt(sufLen)),
              traceUUID,
              UUID.randomUUID(),
              null,
              null,
              // Not root spans will have error tag if ErrorConditions defined
              getTags(traceTypePattern, spanName, 0),
              null));
          spanNumbers--;
        }
      }
    }

    trace.createRandomConnections();
    statistics.offer(traceTypePattern.traceTypeName, trace, traceDuration);
    return trace;
  }

  private List<Pair<String, String>> getTags(@Nonnull TraceTypePattern pattern,
                                             @Nonnull String spanName, int errorRate) {
    final List<Pair<String, String>> tags = new LinkedList<>();

    // add all mandatory tags
    pattern.mandatoryTags.forEach(tag -> tags.add(new Pair<>(tag.tagName, tag.getRandomValue())));

    // add some of optional tags if exist
    if (pattern.optionalTagsPercentage > 0 && pattern.optionalTags != null) {
      pattern.optionalTags.forEach(tag -> {
        if (RANDOM.nextInt(HUNDRED_PERCENT) + 1 <= pattern.optionalTagsPercentage) {
          tags.add(new Pair<>(tag.tagName, tag.getRandomValue()));
        }
      });
    }

    if (isEffectivePercentage(pattern.debugRate)) {
      tags.add(DEBUG_TAG);
    }

    // Check error conditions.
    // If user sets errorConditions for the trace type it will override
    // the common errorRate. Otherwise the common errorRate will be applied to the first span.
    if (pattern.errorConditions != null) {
      errorRate = getErrorRate(spanName, tags, pattern.errorConditions);
    }

    if (isEffectivePercentage(errorRate)) {
      tags.add(ERROR_TAG);
    }

    return tags;
  }

  /**
   * Normalize all distributions. ie trace type, spans counts and so on.
   */
  private void normalizeDistributions(@Nonnull List<TraceTypePattern> traceTypePatterns) {
    // trace types and spans count distribution
    final double tracePercRatio = getNormalizationRatio(traceTypePatterns.stream().
        mapToDouble(t -> t.tracePercentage).sum());

    traceTypePatterns.forEach(traceType -> {
      traceType.tracePercentage = (int) Math.round(traceType.tracePercentage * tracePercRatio);
      normalizeCanonicalDistributions(traceType.spansDistributions);
      normalizeCanonicalDistributions(traceType.traceDurations);
      normalizeCanonicalDistributions(traceType.spansDurations);
    });
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
}