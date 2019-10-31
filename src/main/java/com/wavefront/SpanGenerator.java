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
      // get next items based on distributions
      TraceTypePattern traceTypePattern = getNextTraceType(config.getTraceTypes());
      Distribution spansDistribution = getNextSpanDistribution(traceTypePattern);

      spanQueue.addTrace(generateTrace(traceTypePattern, spansDistribution));
    }
    LOGGER.info("Generation complete!");
    return spanQueue;
  }

  private List<List<Span>> generateTrace(@Nonnull TraceTypePattern pattern,
                                         @Nonnull Distribution spanDistribution) {

    int levels = pattern.nestingLevel;
    int spanNumbers = spanDistribution.getValue() - 1;

    long spanDuration = RANDOM.nextInt(200) + 1; // FIXME make distribution
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
        pattern.traceTypeName,
        currentTime,
        spanDuration,
        "localhost", // + suffixes.charAt(rand.nextInt(sufLen)),
        traceUUID,
        UUID.randomUUID(),
        null,
        null,
        getTags(pattern.errorRate),
        null));

    while (spanNumbers > 0) {
      for (int n = 1; n < levels && spanNumbers > 0; n++) {
        for (int m = n; m < levels && spanNumbers > 0; m++) {
          trace.get(m).add(new Span(
              "name_" + suffixes.charAt(RANDOM.nextInt(sufLen)),
              currentTime,
              spanDuration,
              "localhost", // + suffixes.charAt(rand.nextInt(sufLen)),
              traceUUID,
              UUID.randomUUID(),
              null,
              null,
              getTags(0), //FIXME Errors only in the first span
              null));
          spanNumbers--;
          currentTime += spanDuration;
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

  private List<Pair<String, String>> getTags(int errorRate) {
    // TODO: now it looks static, but for RCA we will generate tags variation for spans
    List<Pair<String, String>> tags = new LinkedList<>();
    tags.add(new Pair<>("application", "trace loader"));
    tags.add(new Pair<>("service", "generator"));
    tags.add(new Pair<>("host", "ip-10.20.30.40"));

    if (errorRate > 0) {
      if (RANDOM.nextInt(100) < errorRate) {
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
      double spansPercRatio = getNormalizationRatio(traceType.spansDistributions.stream().
          mapToDouble(d -> d.percentage).sum());
      traceType.spansDistributions.forEach(d ->
          d.percentage = (int) Math.round(d.percentage * spansPercRatio));
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
   * Get nex trace type for generation based on trace type distributions.
   *
   * @param traceTypePatterns List of trace types to be generated.
   */
  private TraceTypePattern getNextTraceType(@Nonnull List<TraceTypePattern> traceTypePatterns) {
    return traceTypePatterns.get(getIndexOfNextItem(tracePercentages));
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
