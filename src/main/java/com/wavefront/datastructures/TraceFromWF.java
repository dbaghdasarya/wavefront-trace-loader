package com.wavefront.datastructures;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TraceFromWF {
  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

  public String traceId;
  public long start_ms;
  public long end_ms;
  public long total_duration_ms;
  public List<SpanFromWF> spans;

  public void shiftTrace(long deltaMillis) {
    if (spans == null) {
      return;
    }
    spans.forEach(span -> span.startMs += deltaMillis);
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
