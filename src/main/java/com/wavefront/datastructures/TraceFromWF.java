package com.wavefront.datastructures;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

  private String traceId;
  private long start_ms = Long.MAX_VALUE;
  private long end_ms = Long.MIN_VALUE;
  private long total_duration_ms;
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
    return start_ms;
  }

  @JsonIgnore
  public long getEndMs() {
    return end_ms;
  }

  @JsonIgnore
  public long getTotalDurationMs() {
    return end_ms - start_ms;
  }

  private void setStartAndEnd() {
    // Reset start, end and duration.
    start_ms = Long.MAX_VALUE;
    end_ms = Long.MIN_VALUE;
    total_duration_ms = 0;

    if (spans != null && !spans.isEmpty()) {
      spans.forEach(span -> {
        start_ms = Math.min(start_ms, span.getStartMs());
        end_ms = Math.max(end_ms, span.getStartMs() + span.getDurationMs());
      });
      total_duration_ms = end_ms - start_ms;
    }
  }

  /**
   * Shift trace start time. Traces older than 15 minutes are rejected by the Wavefront.
   *
   * @param deltaMillis Delta interval for shifting.
   */
  public void shiftTrace(long deltaMillis) {
    // Reset start, end and duration.
    start_ms = Long.MAX_VALUE;
    end_ms = Long.MIN_VALUE;
    total_duration_ms = 0;

    if (spans != null && !spans.isEmpty()) {
      spans.forEach(span -> {
        span.shiftStartMs(deltaMillis);
        start_ms = Math.min(start_ms, span.getStartMs());
        end_ms = Math.max(end_ms, span.getStartMs() + span.getDurationMs());
      });
      total_duration_ms = end_ms - start_ms;
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
          if (key.equals("parent")
              || key.equals("followsFrom")
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