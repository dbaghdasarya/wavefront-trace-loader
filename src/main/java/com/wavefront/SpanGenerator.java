package com.wavefront;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;

import com.wavefront.TraceTypePattern.Distribution;
import com.wavefront.config.GeneratorConfig;
import com.wavefront.sdk.common.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

/**
 * TODO
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public class SpanGenerator {
  private static final Logger LOGGER = Logger.getLogger(SpanSender.class.getCanonicalName());
  private static final Random RANDOM = new Random(System.currentTimeMillis());
  private static final int HUNDRED_PERCENT = 100;
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
  private List<Integer> tracePercentages;

  public SpanQueue generate(GeneratorConfig config) {
    SpanQueue spanQueue = new SpanQueue();
    int spansCount = config.getSpansRate() * (int) config.getDuration().toSeconds();
    LOGGER.info("Should be generated " + spansCount + " spans.");

    // normalize percentages of distribution to fix wrong inputs
    normalizeDistributions(config.getTraceTypes());
    tracePercentages = config.getTraceTypes().stream().
        map(traceTypePattern -> traceTypePattern.tracePercentage).collect(Collectors.toList());

    while (spanQueue.size() < spansCount) {
      // get next trace type to be generated
      TraceTypePattern traceTypePattern = getNextTraceType(config.getTraceTypes());

      spanQueue.addTrace(generateTrace(traceTypePattern));
    }
    LOGGER.info("Generation complete!");
    return spanQueue;
  }

  /**
   * Generate trace for a given trace type.
   */
  private List<List<Span>> generateTrace(@Nonnull TraceTypePattern traceType) {
    int levels = traceType.nestingLevel;
    int spanNumbers = getNextSpanDistribution(traceType).getValue() - 1;

    int traceDuration = getNextTraceDuration(traceType).getValue();
    // currently all spans have the same duration, expect last one to ensure expected duration
    int spanDuration = spanNumbers == 0 ? traceDuration : traceDuration / spanNumbers;
    int lastSpanDuration = spanNumbers == 0 ? 0 : traceDuration % spanNumbers;

    long currentTime = System.currentTimeMillis();
    String suffixes = "abcdefg";
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

          // the remaining part of duration will be added to last span
          if (spanNumbers == 1) {
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
              getTags(traceType, 0), //FIXME Errors only in the first span
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

    if (errorRate > 0) {
      if (RANDOM.nextInt(HUNDRED_PERCENT) + 1 < errorRate) {
        tags.add(new Pair<>("error", "true"));
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
}
