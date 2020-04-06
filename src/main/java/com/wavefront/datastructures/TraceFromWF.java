package com.wavefront.datastructures;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    setStartAndEnd();
    if (this.spans == null || this.spans.size() == 0) {
      traceId = null;
    } else {
      traceId = spans.get(0).traceId;
    }
  }

  public long getStart_ms() {
    if (start_ms == Long.MAX_VALUE) {
      setStartAndEnd();
    }
    return start_ms;
  }

  public long getEnd_ms() {
    if (end_ms == Long.MIN_VALUE) {
      setStartAndEnd();
    }
    return end_ms;
  }

  public long getTotal_duration_ms() {
    return end_ms - start_ms;
  }

  private void setStartAndEnd() {
    if (spans == null) {
      return;
    }
    spans.forEach(span -> {
      start_ms = Math.min(start_ms, span.startMs);
      end_ms = Math.max(end_ms, span.startMs + span.durationMs);
    });
    total_duration_ms = end_ms - start_ms;
  }

  public void shiftTrace(long deltaMillis) {
    start_ms = Long.MAX_VALUE;
    end_ms = Long.MIN_VALUE;
    if (spans == null) {
      return;
    }

    spans.forEach(span -> {
      span.startMs += deltaMillis;
      start_ms = Math.min(start_ms, span.startMs);
      end_ms = Math.max(end_ms, span.startMs + span.durationMs);
    });
  }

  public void updateUUIDs() {
    if (spans == null) {
      return;
    }
    final Map<String, String> uuids = new HashMap<>();
    spans.forEach(span -> {
      span.spanId = uuids.computeIfAbsent(span.spanId, k -> UUID.randomUUID().toString());
      span.traceId = uuids.computeIfAbsent(span.traceId, k -> UUID.randomUUID().toString());

      span.annotations.forEach(a ->
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
    return JSON_MAPPER.writeValueAsString(this) + "\n";
  }
}