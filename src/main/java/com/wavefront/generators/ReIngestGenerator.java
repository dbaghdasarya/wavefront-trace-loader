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
import java.util.stream.Stream;


public class ReIngestGenerator extends SpanGenerator {
  private final Statistics statistics = new Statistics();
  private final String sourceFile;
  private final DataQueue dataQueue;


  public ReIngestGenerator(String sourceFile, DataQueue dataQueue) {
    this.sourceFile = sourceFile;
    this.dataQueue = dataQueue;
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
    long endMillis = reGenerateTraces(false, null, 0);
//    try {
//      long shift = System.currentTimeMillis();
//      if (endMillis > shift) {
//        shift = endMillis - shift;
//      } else {
//        shift = 0;
//      }
//      Thread.sleep(shift + 7_200_000);
//    } catch (InterruptedException e) {
//      LOGGER.severe(e.toString());
//    }
//    reGenerateTraces(false,
//        new Pair<String, String>("service", "preference_service"), 80);
  }

  private long reGenerateTraces(boolean isToFile, Pair<String, String> tagAndValue,
                                int percentage) {
    AtomicInteger counter = new AtomicInteger(0);
    AtomicLong startMoment = new AtomicLong(System.currentTimeMillis());
    AtomicLong endMoment = new AtomicLong(startMoment.get());
    AtomicLong atomicDelta = new AtomicLong();
    ObjectMapper objectMapper = new ObjectMapper();
    AtomicLong start_ms = new AtomicLong(System.currentTimeMillis());
    try (Stream<String> stream = Files.lines(Paths.get(sourceFile))) {
      stream.forEach(line -> {
        if (isStart(line)) {
          int startPos = line.indexOf(' ');
          if (startPos >= 0) {
            start_ms.set(Long.parseLong(line.substring(startPos + 1)));
            atomicDelta.set(0);
          }
        } else if (isTrace(line)) {
          int startPos = line.indexOf('{');
          if (startPos >= 0) {
            line = line.substring(startPos);
            try {
              TraceFromWF traceFromWF =
                  objectMapper.readValue(line, new TypeReference<TraceFromWF>() {
                  });
              // Add conditional errors
              if (tagAndValue != null && percentage > 0) {
                traceFromWF.spans.forEach(span -> {
                  span.annotations.forEach(map -> {
                    if (RANDOM.nextInt(HUNDRED_PERCENT) + 1 <= percentage &&
                        map.entrySet().stream().
                            anyMatch(entry -> entry.getKey().equals(tagAndValue._1) &&
                                entry.getValue().equals(tagAndValue._2))) {
                      map.put("error", "true");
                    }
                  });
                });
              }

              // Shift trace to the predefined time (can't be older than 15 min)
              long delta = atomicDelta.updateAndGet(value -> value == 0 ?
                  start_ms.get() - traceFromWF.start_ms : value);
              traceFromWF.shiftTrace(delta);
              traceFromWF.updateUUIDs();

              // Convert WFtrace to trace convenient for DataQueue
              Trace trace = new Trace(1);
              traceFromWF.spans.forEach(wfspan -> {
                trace.add(0, wfspan.toSpan());
              });
              dataQueue.addTrace(trace);
              statistics.offer(trace.getSpans().get(0).get(0).getName(),
                  trace, traceFromWF.total_duration_ms);

              long tempStart = traceFromWF.start_ms + delta;
              startMoment.updateAndGet(value -> Math.min(tempStart, value));
              long tempEnd = traceFromWF.end_ms + delta;
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

      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
      String endTime = simpleDateFormat.format(new Date(endMoment.get()));
      LOGGER.info("The number of reIngested traces is " + counter.get() +
          "\nIngestion started at " + simpleDateFormat.format(new Date(startMoment.get())) +
          " and will complete at " + simpleDateFormat.format(new Date(endMoment.get())) +
          " (" + (startMoment.get() / 1000) + " - " + (endMoment.get() / 1000) + ")");
    } catch (IOException e) {
      LOGGER.severe(e.toString());
    }

    return endMoment.get();
  }

  private boolean isTrace(String anyString) {
    return !Strings.isNullOrEmpty(anyString) && anyString.startsWith("data: {\"traceId\":\"");
  }

  private boolean isStart(String anyString) {
    return !Strings.isNullOrEmpty(anyString) && anyString.startsWith("start_ms: ");
  }
}