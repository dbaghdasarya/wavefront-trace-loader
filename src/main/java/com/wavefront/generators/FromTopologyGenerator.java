package com.wavefront.generators;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import com.wavefront.DataQueue;
import com.wavefront.config.GeneratorConfig;
import com.wavefront.datastructures.Distribution;
import com.wavefront.datastructures.Span;
import com.wavefront.datastructures.Trace;
import com.wavefront.helpers.Statistics;
import com.wavefront.sdk.common.Pair;
import com.wavefront.topology.TraceTopology;
import com.wavefront.datastructures.TraceType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import static com.wavefront.datastructures.Distribution.HUNDRED_PERCENT;
import static com.wavefront.datastructures.Distribution.getIndexOfNextItem;
import static com.wavefront.datastructures.ErrorCondition.getErrorRate;
import static com.wavefront.helpers.Defaults.DEBUG_TAG;
import static com.wavefront.helpers.Defaults.ERROR;
import static com.wavefront.helpers.Defaults.ERROR_TAG;
import static com.wavefront.helpers.Defaults.SERVICE;
import static com.wavefront.helpers.WftlUtils.getRandomFromSet;
import static com.wavefront.helpers.WftlUtils.isEffectivePercentage;

/**
 * Class generates traces based on {@link GeneratorConfig} parameters and topology definition.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public class FromTopologyGenerator extends SpanGenerator {
  private static final Logger LOGGER =
      Logger.getLogger(FromTopologyGenerator.class.getCanonicalName());

  private final Statistics statistics = new Statistics();
  @Nonnull
  private final GeneratorConfig generatorConfig;
  private final TraceType[] traceTypes = new TraceType[100];
  private final LoadingCache<TraceType, List<Integer>> traceDurationsPercentages =
      CacheBuilder.newBuilder().expireAfterAccess(2, TimeUnit.MINUTES).
          build(new CacheLoader<>() {
            @Override
            public List<Integer> load(@Nonnull TraceType traceType) {
              return traceType.traceDurations.stream().map(distribution ->
                  distribution.percentage).collect(Collectors.toList());
            }
          });

  private TraceTopology traceTopology;
  private Map<TraceType, Trace> traceTemplates;


  public FromTopologyGenerator(@Nonnull GeneratorConfig config, @Nonnull DataQueue dataQueue) {
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

  private void generateTraceTemplates() {
    traceTemplates = new HashMap<>();
    int current = 0;
    TraceType lastTraceType = null;
    for (TraceType tt : traceTopology.traceTypes) {
      // Calculate nesting levels number. Spans distributed across levels in geometrical sequence.
      int temp = tt.spansCount;
      int levels = 0;
      while (temp > 0) {
        levels++;
        temp = temp >> 1;
      }
      final UUID traceUUID = UUID.randomUUID();
      final Trace trace = new Trace(levels, traceUUID);
      int alreadyGenerated = 1; // root span.

      // Root span.
      final String root = traceTopology.getRandomRootService();
      Set<String> nextLevelServices = Set.of(root);
      trace.add(0, new Span(
          traceTopology.getSpanName(root),
          0,
          0,
          "localhost", // + suffixes.charAt(rand.nextInt(sufLen)),
          traceUUID,
          UUID.randomUUID(),
          null,
          null,
          List.of(new Pair<>(SERVICE, root)),
          null));
      for (int n = 1; n < levels && alreadyGenerated < tt.spansCount; n++) {
        final int max = Math.min(1 << n, tt.spansCount - alreadyGenerated);
        alreadyGenerated += max;

        final Set<String> previousLevelServices = nextLevelServices;
        nextLevelServices = new HashSet<>();

        for (int m = 0; m < max; m++) {
          final String nextService = traceTopology.getNextLevelService(previousLevelServices);
          nextLevelServices.add(nextService);
          if (Strings.isNullOrEmpty(nextService)) {
            alreadyGenerated = tt.spansCount;
            break;
          }

          final UUID parentUUID = getRandomParentUUID(trace, n - 1, nextService);
          if (parentUUID == null) {
            LOGGER.severe("Exceptional issue in trace generation!");
            break;
          }

          trace.add(n, new Span(
              traceTopology.getSpanName(nextService),
              0,
              0,
              "localhost", // + suffixes.charAt(rand.nextInt(sufLen)),
              traceUUID,
              UUID.randomUUID(),
              List.of(parentUUID),
              null,
              // Not root spans will have error tag if ErrorConditions defined
              List.of(new Pair<>(SERVICE, nextService)),
              null));
        }
      }

      traceTemplates.put(tt, trace);
      lastTraceType = tt;
      final int max = (int) Math.min(HUNDRED_PERCENT, current + tt.tracePercentage);
      for (int n = current; n < max; n++) {
        traceTypes[n] = tt;
      }
      current = max;
    }
    if (lastTraceType != null && current < 99) {
      for (int n = current; n < 100; n++) {
        traceTypes[n] = lastTraceType;
      }
    }
  }

  private List<Pair<String, String>> getTags(@Nonnull Trace trace, @Nonnull TraceType traceType,
                                             int level, String service, String spanName,
                                             UUID parentUUID) {
    final List<Pair<String, String>> tags = traceTopology.getServiceTags(service);
    assert tags != null;

    if (isEffectivePercentage(traceType.debugRate)) {
      tags.add(DEBUG_TAG);
    }

    if (traceType.errorConditions == null || traceType.errorConditions.isEmpty()) {
      // In case errorConditions don't set, only the root span will be marked as error.
      if (level == 0 && isEffectivePercentage(traceType.errorRate)) {
        tags.add(ERROR_TAG);
      }
    } else if (isEffectivePercentage(getErrorRate(spanName, tags, traceType.errorConditions))) {
      tags.add(ERROR_TAG);

      // Escalate error to the parent spans.
      AtomicReference<UUID> atomicParent = new AtomicReference<>(parentUUID);
      for (int n = level - 1; n >= 0 && atomicParent.get() != null; n--) {
        final Span parentSpan = trace.getSpans().get(n).stream().
            filter(span -> span.getSpanUUID().equals(atomicParent.get())).
            findAny().orElse(null);

        if (parentSpan != null) {
          assert parentSpan.getTags() != null;
          // Interrupt error escalation if the parent span already erroneous.
          if (parentSpan.getTags().stream().anyMatch(p -> p._1.equals(ERROR))) {
            break;
          }
          parentSpan.getTags().add(ERROR_TAG);
          if (parentSpan.getParents() != null && !parentSpan.getParents().isEmpty()) {
            atomicParent.set(parentSpan.getParents().get(0));
          } else {
            atomicParent.set(null);
          }
        }
      }
    }

    return tags;
  }

  private UUID getRandomParentUUID(@Nonnull Trace trace, int level, String service) {
    if (level >= trace.getSpans().size()) {
      return null;
    }

    final Set<UUID> parents = new HashSet<>();
    trace.getSpans().get(level).forEach(span -> {
      if (span.getTags() != null && !span.getTags().isEmpty() &&
          traceTopology.isParent(service, span.getTags().get(0)._2)) {
        parents.add(span.getSpanUUID());
      }
    });

    if (parents.isEmpty()) {
      return null;
    } else {
      return getRandomFromSet(parents);
    }
  }

  @Override
  public Statistics getStatistics() {
    return statistics;
  }

  @Override
  protected void initGeneration() {
    traceTopology = generatorConfig.getTraceTopology();
    generateTraceTemplates();
  }

  @Override
  protected Trace generateTrace(long startMillis) {
    final TraceType traceType = traceTypes[RANDOM.nextInt(HUNDRED_PERCENT)];
    final Distribution durationDistribution = traceType.traceDurations.get(
        getIndexOfNextItem(traceDurationsPercentages.getUnchecked(traceType)));
    final long traceDuration = durationDistribution.startValue +
        RANDOM.nextInt(durationDistribution.endValue - durationDistribution.startValue);

    final Trace traceTemplate = traceTemplates.get(traceType);
    if (traceTemplate == null) {
      return null;
    }

    final Map<UUID, UUID> uuids = new HashMap<>();

    final UUID traceUUID = UUID.randomUUID();
    final Trace trace = new Trace(traceTemplate.getLevels(), traceUUID);
    final Span root = traceTemplate.getSpans().get(0).get(0);
    assert root.getTags() != null;
    trace.add(0, new Span(
        root.getName(),
        startMillis,
        traceDuration,
        "localhost",
        traceUUID,
        uuids.computeIfAbsent(root.getSpanUUID(), k -> UUID.randomUUID()),
        null,
        null,
        getTags(trace, traceType, 0, root.getTags().get(0)._2, root.getName(), null),
        null
    ));
    for (int n = 1; n < traceTemplate.getSpans().size(); n++) {
      for (int m = 0; m < traceTemplate.getSpans().get(n).size(); m++) {
        final Span span = traceTemplate.getSpans().get(n).get(m);
        assert span.getParents() != null;
        final Span templateParentSpan = traceTemplate.getSpans().get(n - 1).stream().
            filter(s -> s.getSpanUUID().equals(span.getParents().get(0))).findAny().
            orElse(null);
        if (templateParentSpan == null) {
          return null;
        }

        final UUID parentUUID = uuids.computeIfAbsent(templateParentSpan.getSpanUUID(),
            k -> UUID.randomUUID());
        final Span parentSpan = trace.getSpans().get(n - 1).stream().
            filter(s -> s.getSpanUUID().equals(parentUUID)).findAny().orElse(null);
        if (parentSpan == null) {
          return null;
        }

        final long halfDuration = parentSpan.getDuration() / 2;
        if (halfDuration == 0) {
          return null;
        }
        final long duration = halfDuration + (long) (RANDOM.nextDouble() * halfDuration);
        final long spanStartMillis = parentSpan.getStartMillis() +
            (long) (RANDOM.nextDouble() * (parentSpan.getDuration() - duration));
        assert span.getTags() != null;
        trace.add(n, new Span(
            span.getName(),
            spanStartMillis,
            duration,
            "localhost",
            traceUUID,
            uuids.computeIfAbsent(span.getSpanUUID(), k -> UUID.randomUUID()),
            List.of(parentUUID),
            null,
            getTags(trace, traceType, n, span.getTags().get(0)._2, span.getName(), parentUUID),
            null
        ));
      }
    }

    statistics.offer(root.getName(), trace, traceDuration);
    return trace;
  }
}