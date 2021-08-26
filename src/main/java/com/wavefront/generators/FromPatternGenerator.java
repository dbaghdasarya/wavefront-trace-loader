package com.wavefront.generators;

import com.wavefront.DataQueue;
import com.wavefront.SpanSender;
import com.wavefront.config.GeneratorConfig;
import com.wavefront.datastructures.DistributionIterator;
import com.wavefront.datastructures.ExactDistributionIterator;
import com.wavefront.datastructures.RandomDistributionIterator;
import com.wavefront.datastructures.ReferenceDistribution;
import com.wavefront.datastructures.Span;
import com.wavefront.datastructures.SpanKind;
import com.wavefront.datastructures.Trace;
import com.wavefront.datastructures.TraceTypePattern;
import com.wavefront.datastructures.ValueDistribution;
import com.wavefront.helpers.Statistics;
import com.wavefront.sdk.common.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import static com.wavefront.datastructures.ErrorCondition.getErrorRate;
import static com.wavefront.helpers.Defaults.DEBUG_TAG;
import static com.wavefront.helpers.Defaults.ERROR_TAG;
import static com.wavefront.helpers.Defaults.HUNDRED_PERCENT;
import static com.wavefront.helpers.Defaults.PATTERN;
import static com.wavefront.helpers.WftlUtils.isEffectivePercentage;

/**
 * Class generates traces based on {@link GeneratorConfig} parameters.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public class FromPatternGenerator extends TraceGenerator {
  private static final Logger LOGGER = Logger.getLogger(SpanSender.class.getCanonicalName());
  protected DistributionIterator<ReferenceDistribution<TraceTypePattern>> traceTypePatternIterator;

  public FromPatternGenerator(@Nonnull GeneratorConfig config, @Nonnull DataQueue dataQueue) {
    super(dataQueue);
    this.generatorConfig = config;
  }

  @Override
  public void generateForFile() {
    startGeneration(false, generatorConfig, LOGGER, PATTERN);
  }

  @Override
  public void run() {
    startGeneration(true, generatorConfig, LOGGER, PATTERN);
  }

  @Override
  protected void initGeneration() {
    List<ReferenceDistribution<TraceTypePattern>> referenceDistributions = generatorConfig.
        getTraceTypePatterns().stream().map(traceTypePattern -> new ReferenceDistribution<>(
        traceTypePattern, traceTypePattern.tracePercentage)).collect(Collectors.toList());
    if (generatorConfig.getTotalTraceCount() > 0) {
      traceTypePatternIterator = new ExactDistributionIterator<>(referenceDistributions,
          generatorConfig.getTotalTraceCount());
      referenceDistributions.forEach(traceTypePattern -> {
        traceTypePattern.reference.init((int) Math.round(traceTypePattern.portion));
      });
    } else {
      traceTypePatternIterator = new RandomDistributionIterator<>(referenceDistributions);
      referenceDistributions.forEach(traceTypePattern -> {
        traceTypePattern.reference.init(0);
      });
    }
  }

  @Override
  public Statistics getStatistics() {
    return statistics;
  }

  @Override
  protected Trace generateTrace(long startMillis) {
    // get next trace type to be generated
    ReferenceDistribution<TraceTypePattern> traceTypePatternDistribution =
        traceTypePatternIterator.getNextDistribution();
    if (traceTypePatternDistribution == null) {
      return null;
    }
    final TraceTypePattern traceTypePattern = traceTypePatternDistribution.reference;
    final int levels = traceTypePattern.nestingLevel;
    ValueDistribution spanDistribution = traceTypePattern.getNextSpanDistribution();
    if (spanDistribution == null) {
      return null;
    }
    int spanNumbers = spanDistribution.getValue() - 1;
    int spanDuration;
    int lastSpanDuration = 0;
    boolean useSpansDistribution = false;
    int traceDuration = 0;

    // traceDurations has priority,so if it is set spansDurations is skipped
    if (!traceTypePattern.traceDurations.isEmpty()) {
      ValueDistribution traceDurationDistribution = traceTypePattern.getNextTraceDuration();
      if (traceDurationDistribution == null) {
        return null;
      }
      traceDuration = traceDurationDistribution.getValue();
      // currently all spans have the same duration, expect last one to ensure expected duration
      spanDuration = spanNumbers == 0 ? traceDuration : traceDuration / spanNumbers;
      lastSpanDuration = spanNumbers == 0 ? 0 : traceDuration % spanNumbers;
    } else {
      ValueDistribution spanDurationDistribution = traceTypePattern.getNextSpanDuration();
      if (spanDurationDistribution == null) {
        return null;
      }
      useSpansDistribution = true;
      spanDuration = spanDurationDistribution.getValue();
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
    trace.setRoot(traceTypePattern.traceTypeName);

    while (spanNumbers > 0) {
      for (int n = 1; n < levels && spanNumbers > 0; n++) {
        for (int m = n; m < levels && spanNumbers > 0; m++) {
          startMillis += spanDuration;

          if (useSpansDistribution) {
            ValueDistribution spanDurationDistribution = traceTypePattern.getNextSpanDuration();
            if (spanDurationDistribution == null) {
              return null;
            }
            spanDuration = spanDurationDistribution.getValue();
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
                                             @Nonnull String spanName, double errorRate) {
    final List<Pair<String, String>> tags = new LinkedList<>();

    // add all mandatory tags
    pattern.mandatoryTags.forEach(tag -> tags.add(new Pair<>(tag.tagName, tag.getRandomValue())));

    // add some of optional tags if exist
    if (pattern.optionalTagsPercentage > 0 && pattern.optionalTags != null) {
      pattern.optionalTags.forEach(tag -> {
        if (RANDOM.nextDouble() <= (pattern.optionalTagsPercentage / HUNDRED_PERCENT)) {
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
}