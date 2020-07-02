package com.wavefront.datastructures;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.wavefront.helpers.Defaults.FOLLOWS_FROM;
import static com.wavefront.helpers.Defaults.PARENT;

/**
 * An intermediate data structure for loading and parsing JSON traces exported from the Wavefront
 * GUI to traces used by the TraceLoader tool, and vice versa. Names of some fields don't match
 * commonly used Java naming notations, but it is necessary for smooth converting from/to JSON.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class TraceFromWF {
  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

  @SuppressWarnings("unused")
  private String traceId;
  @JsonProperty("start_ms")
  private long startMs = Long.MAX_VALUE;
  @JsonProperty("end_ms")
  private long endMs = Long.MIN_VALUE;
  @JsonProperty("total_duration_ms")
  private long totalDurationMs;
  private List<SpanFromWF> spans;

  public List<SpanFromWF> getSpans() {
    return spans;
  }

  public void setSpans(List<SpanFromWF> spans) {
    this.spans = spans;
    if (this.spans == null || this.spans.isEmpty()) {
      traceId = null;
    } else {
      traceId = spans.get(0).getTraceId();
    }

    // In any case, this method should be called to update the start and end in accordance with
    // the new set of spans (even this set is empty).
    setStartAndEnd();
  }

  @JsonIgnore
  public long getStartMs() {
    return startMs;
  }

  @JsonIgnore
  public long getEndMs() {
    return endMs;
  }

  @JsonIgnore
  public long getTotalDurationMs() {
    return totalDurationMs;
  }

  private void setStartAndEnd() {
    // Reset start, end and duration.
    startMs = Long.MAX_VALUE;
    endMs = Long.MIN_VALUE;
    totalDurationMs = 0;

    if (spans != null && !spans.isEmpty()) {
      spans.forEach(span -> {
        startMs = Math.min(startMs, span.getStartMs());
        endMs = Math.max(endMs, span.getStartMs() + span.getDurationMs());
      });
      totalDurationMs = endMs - startMs;
    }
  }

  /**
   * Shift trace start time. Traces older than 15 minutes are rejected by the Wavefront.
   *
   * @param deltaMillis Delta interval for shifting.
   */
  public void shiftTrace(long deltaMillis) {
    // Reset start, end and duration.
    startMs = Long.MAX_VALUE;
    endMs = Long.MIN_VALUE;
    totalDurationMs = 0;

    if (spans != null && !spans.isEmpty()) {
      spans.forEach(span -> {
        span.shiftStartMs(deltaMillis);
        startMs = Math.min(startMs, span.getStartMs());
        endMs = Math.max(endMs, span.getStartMs() + span.getDurationMs());
      });
      totalDurationMs = endMs - startMs;
    }
  }

  /**
   * For the re-ingestion of traces, the UUIDs should be changed for don't mix them with already
   * ingested traces.
   */
  public void updateUUIDs() {
    if (spans == null || spans.isEmpty()) {
      return;
    }
    final Map<String, String> uuids = new HashMap<>();
    spans.forEach(span -> {
      span.setSpanId(uuids.computeIfAbsent(span.getSpanId(), k -> UUID.randomUUID().toString()));
      span.setTraceId(uuids.computeIfAbsent(span.getTraceId(), k -> UUID.randomUUID().toString()));

      span.getAnnotations().forEach(a ->
      {
        for (Map.Entry<String, String> entry : a.entrySet()) {
          String key = entry.getKey();
          if (key.equals(PARENT)
              || key.equals(FOLLOWS_FROM)
              || key.equals("spanId")
              || key.equals("traceId")) {
            entry.setValue(uuids.computeIfAbsent(entry.getValue(),
                k -> UUID.randomUUID().toString()));
          }
        }
      });
    });
  }

  public String toJSONString() throws Exception {
    return JSON_MAPPER.writeValueAsString(this);
  }
}