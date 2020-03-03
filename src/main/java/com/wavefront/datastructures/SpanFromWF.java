package com.wavefront.datastructures;

import com.wavefront.sdk.common.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class SpanFromWF {
  public String name;
  public String host;
  public long startMs;
  public long durationMs;
  public String spanId;
  public String traceId;
  public List<Map<String, String>> annotations;

  public Span toSpan() {
    List<Pair<String, String>> tags = new LinkedList<>();
    annotations.forEach(a -> a.forEach((k, v) -> tags.add(new Pair<>(k, v))));

    List<UUID> parents = tags.stream().filter(pair -> pair._1.equals("parent")).
        map(id -> UUID.fromString(id._2)).collect(Collectors.toList());

    List<UUID> followsFrom = tags.stream().filter(pair -> pair._1.equals("followsFrom")).
        map(id -> UUID.fromString(id._2)).collect(Collectors.toList());

    return new Span(name, startMs, durationMs, host, UUID.fromString(traceId),
        UUID.fromString(spanId), parents, followsFrom, tags, null);
  }
}