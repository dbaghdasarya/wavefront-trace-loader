package com.wavefront.generators;

import com.google.common.base.Strings;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wavefront.DataQueue;
import com.wavefront.datastructures.Trace;
import com.wavefront.datastructures.TraceFromWF;
import com.wavefront.helpers.Statistics;
import com.wavefront.sdk.common.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import static com.wavefront.helpers.Defaults.HUNDRED_PERCENT;
import static com.wavefront.helpers.Defaults.ERROR;

/**
 * Class re-ingests already generated traces from file.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public class ReIngestGenerator extends BasicGenerator {
  private static final Logger LOGGER = Logger.getLogger(ReIngestGenerator.class.getCanonicalName());
  private final String sourceFile;


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
    ObjectMapper objectMapper = new ObjectMapper();
    AtomicLong start_ms = new AtomicLong(System.currentTimeMillis());
    try (Stream<String> stream = Files.lines(Paths.get(sourceFile))) {
      stream.forEach(line -> {
        if (isStart(line)) {
          final int startPos = line.indexOf(' ');
          if (startPos >= 0) {
            start_ms.set(Long.parseLong(line.substring(startPos + 1)));
            atomicDelta.set(0);
          }
        } else if (isTrace(line)) {
          final int startPos = line.indexOf('{');
          if (startPos >= 0) {
            line = line.substring(startPos);
            try {
              final TraceFromWF traceFromWF =
                  objectMapper.readValue(line, new TypeReference<TraceFromWF>() {
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

              // Shift trace to the predefined time (can't be older than 15 min)
              final long delta = atomicDelta.updateAndGet(value -> value == 0 ?
                  start_ms.get() - traceFromWF.getStartMs() : value);
              traceFromWF.shiftTrace(delta);
              traceFromWF.updateUUIDs();

              // Convert WFtrace to trace convenient for DataQueue
              final Trace trace = new Trace(1, null);
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

  private boolean isTrace(String anyString) {
    return !Strings.isNullOrEmpty(anyString) && anyString.startsWith("data: {\"traceId\":\"");
  }

  private boolean isStart(String anyString) {
    return !Strings.isNullOrEmpty(anyString) && anyString.startsWith("start_ms: ");
  }
}