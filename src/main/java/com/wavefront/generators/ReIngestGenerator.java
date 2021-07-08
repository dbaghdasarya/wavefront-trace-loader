package com.wavefront.generators;

import com.google.common.base.Strings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wavefront.DataQueue;
import com.wavefront.datastructures.SpanFromWF;
import com.wavefront.datastructures.Trace;
import com.wavefront.datastructures.TraceFromWF;
import com.wavefront.helpers.Statistics;
import com.wavefront.sdk.common.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import static com.wavefront.helpers.Defaults.ERROR;
import static com.wavefront.helpers.Defaults.FOLLOWS_FROM;
import static com.wavefront.helpers.Defaults.HUNDRED_PERCENT;
import static com.wavefront.helpers.Defaults.PARENT;

/**
 * Class re-ingests already generated traces from file.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public class ReIngestGenerator extends BasicGenerator {
  private static final Logger LOGGER = Logger.getLogger(ReIngestGenerator.class.getCanonicalName());
  private static final String START = "start_ms: ";
  private static final String LATENCY = "latency: ";
  private final String sourceFile;
  private final LinkedList<LatencyCondition> latencies = new LinkedList<>();
  private final ObjectMapper objectMapper = new ObjectMapper();

  public ReIngestGenerator(String sourceFile, @Nonnull DataQueue dataQueue) {
    super(dataQueue);
    this.sourceFile = sourceFile;
  }

  @Override
  public void generateForFile() {
    reGenerateTraces(true, null, 0);
  }

  @Override
  public Statistics getStatistics() {
    return statistics;
  }

  @Override
  public void run() {
    reGenerateTraces(false, null, 0);
  }

  private void reGenerateTraces(boolean isToFile, Pair<String, String> tagAndValue,
                                double percentage) {
    AtomicInteger counter = new AtomicInteger(0);
    AtomicLong startMoment = new AtomicLong(System.currentTimeMillis());
    AtomicLong endMoment = new AtomicLong(startMoment.get());
    AtomicLong atomicDelta = new AtomicLong();
    AtomicLong start_ms = new AtomicLong(System.currentTimeMillis());
    latencies.clear();
    try (Stream<String> stream = Files.lines(Paths.get(sourceFile))) {
      stream.forEach(line -> {
        if (isStart(line)) {
          final int startPos = line.indexOf(' ');
          if (startPos >= 0) {
            start_ms.set(Long.parseLong(line.substring(startPos + 1)));
            atomicDelta.set(0);
          }
        } else if (isLatency(line)) {
          try {
            final LatencyCondition latencyCondition =
                objectMapper.readValue(line.substring(LATENCY.length()), new TypeReference<>() {
                });
            latencyCondition.delta /= 100.0;
            latencyCondition.probability /= 100.0;

            latencies.add(latencyCondition);
          } catch (JsonProcessingException e) {
            e.printStackTrace();
          }
        } else if ((line = extractTrace(line)) != null) {
          try {
            final TraceFromWF traceFromWF =
                objectMapper.readValue(line, new TypeReference<>() {
                });
            // Add conditional errors
            if (tagAndValue != null && percentage > 0) {
              traceFromWF.getSpans().forEach(span -> span.getAnnotations().forEach(map -> {
                if ((RANDOM.nextDouble() <= percentage / HUNDRED_PERCENT) &&
                    map.entrySet().stream().
                        anyMatch(entry -> entry.getKey().equals(tagAndValue._1) &&
                            entry.getValue().equals(tagAndValue._2))) {
                  map.put(ERROR, "true");
                }
              }));
            }

            // Update latency
            if (!latencies.isEmpty()) {
              traceFromWF.getSpans().forEach(span -> {
                if (span.getSpanId().equals("3c13b2e8-c99a-4fba-bde7-58f1d359fd18")) {
                  int n = 10;
                }
                latencies.forEach(l -> {
                  if (span.getName().equals(l.spanName)) {
                    int index = span.getAnnotations().indexOf(Map.of(l.tagName, l.tagValue));
                    if (index > -1 && RANDOM.nextDouble() < l.probability) {
                      span.multiplyDurationMs(l.delta);
                      fixTraceDuration(traceFromWF, span, l.delta);
                    }
                  }
                });
              });
            }

            // Shift trace to the predefined time (can't be older than 15 min)
            final long delta = atomicDelta.updateAndGet(value -> value == 0 ?
                start_ms.get() - traceFromWF.getStartMs() : value);
            traceFromWF.shiftTrace(delta);

            final Trace trace = new Trace(1, null);
            trace.setRoot(traceFromWF.updateUUIDs());
            // Convert WFtrace to trace convenient for DataQueue
            traceFromWF.getSpans().forEach(wfspan -> {
              trace.add(0, wfspan.toSpan());
            });
            dataQueue.addTrace(trace);
            statistics.offer(trace.getSpans().get(0).get(0).getName(),
                trace, traceFromWF.getTotalDurationMs());

            final long tempStart = traceFromWF.getStartMs();
            startMoment.updateAndGet(value -> Math.min(tempStart, value));
            final long tempEnd = traceFromWF.getEndMs();
            endMoment.updateAndGet(value -> Math.max(tempEnd, value));
            counter.incrementAndGet();
            if (!isToFile) {
              Thread.sleep(0);
            }
          } catch (IOException | InterruptedException e) {
            LOGGER.severe(e.toString());
          }
        }
      });

      final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
      LOGGER.info("The number of reIngested traces is " + counter.get() +
          "\nIngestion started at " + simpleDateFormat.format(new Date(startMoment.get())) +
          " and will complete at " + simpleDateFormat.format(new Date(endMoment.get())) +
          " (" + (startMoment.get() / 1000) + " - " + (endMoment.get() / 1000) + ")");
    } catch (IOException e) {
      LOGGER.severe(e.toString());
    }
  }

  private void fixTraceDuration(TraceFromWF trace, SpanFromWF span, double delta) {
    String parentId = null;
    List<String> parentIds = span.getAnnotations().stream().map(map -> map.get(PARENT))
        .collect(Collectors.toList());
    if (parentIds != null && parentIds.size() > 0) {
      parentId = parentIds.get(0);
    } else {
      parentIds = span.getAnnotations().stream().map(map -> map.get(FOLLOWS_FROM))
          .collect(Collectors.toList());
      if (parentIds != null && parentIds.size() > 0) {
        parentId = parentIds.get(0);
      }
    }

    if (!Strings.isNullOrEmpty(parentId)) {
      String finalParentId = parentId;
      SpanFromWF nextSpan = trace.getSpans().stream().
          filter(s -> s.getSpanId().equals(finalParentId)).findFirst().orElse(null);
      if (nextSpan != null) {
        nextSpan.multiplyDurationMs(delta);
        fixTraceDuration(trace, nextSpan, delta);
      }
    } else {
      trace.multiplyDurationMs(delta);
    }
  }

  private String extractTrace(String anyString) {
    String trace = null;
    if (!Strings.isNullOrEmpty(anyString)) {
      if (anyString.startsWith("data: {\"traceId\":\"")) {
        final int startPos = anyString.indexOf('{');
        if (startPos >= 0) {
          trace = anyString.substring(startPos);
        }
      } else if (anyString.startsWith("[{\"root\":\"")) {
        final int startPos = anyString.indexOf("{\"traceId\":\"");
        final int endPos = anyString.lastIndexOf(']');
        if (startPos >= 0 && endPos >= 0) {
          trace = anyString.substring(startPos, endPos);
        }
      }
    }

    return trace;
  }

  private boolean isStart(String anyString) {
    return !Strings.isNullOrEmpty(anyString) && anyString.startsWith(START);
  }

  private boolean isLatency(String anyString) {
    return !Strings.isNullOrEmpty(anyString) && anyString.startsWith(LATENCY);
  }

  public static class LatencyCondition {
    public String spanName;
    public String tagName;
    public String tagValue;
    public double delta;
    public double probability;
  }
}