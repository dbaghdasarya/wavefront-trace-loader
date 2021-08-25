package com.wavefront.datastructures;

import com.wavefront.sdk.common.Pair;
import com.wavefront.sdk.entities.tracing.SpanLog;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

public class StatSpan extends Span{
  public StatSpan(String name, long startMillis, long durationMillis, @Nullable String source, UUID traceUUID, UUID spanUUID, @Nullable List<UUID> parents, @Nullable List<UUID> followsFrom, @Nullable List<Pair<String, String>> tags, @Nullable List<SpanLog> spanLogs) {
    super(name, startMillis, durationMillis, source, traceUUID, spanUUID, parents, followsFrom,
        tags, spanLogs, SpanKind.STATISTICS);
  }

  @Override
  public String toString() {
    return ":(" + super.toString();
  }
}
