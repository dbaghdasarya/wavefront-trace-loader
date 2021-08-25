package com.wavefront.datastructures;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.wavefront.sdk.common.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.wavefront.helpers.Defaults.FOLLOWS_FROM;
import static com.wavefront.helpers.Defaults.PARENT;

/**
 * An intermediate data structure for loading and parsing JSON spans exported from the Wavefront GUI
 * to spans used by the TraceLoader tool, and vice versa. Names of some fields don't match commonly
 * used Java naming notations, but it is necessary for smooth converting from/to JSON.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SpanFromWF {
  private String name;
  private String host;
  private long startMs;
  private long durationMs;
  private String spanId;
  private String traceId;
  private List<Map<String, String>> annotations;

  public SpanFromWF() {
  }

  public SpanFromWF(String name, String host, long startMs, long durationMs, String spanId,
                    String traceId, List<UUID> parents, List<UUID> followsFrom,
                    List<Pair<String, String>> tags) {
    this.name = name;
    this.host = host;
    this.startMs = startMs;
    this.durationMs = durationMs;
    this.spanId = spanId;
    this.traceId = traceId;
    this.annotations = new LinkedList<>();
    if (parents != null) {
      parents.forEach(p -> this.annotations.add(Map.of(PARENT, p.toString())));
    }
    if (followsFrom != null) {
      followsFrom.forEach(ff -> this.annotations.add(Map.of(FOLLOWS_FROM, ff.toString())));
    }
    if (tags != null) {
      tags.forEach(t -> this.annotations.add(Map.of(t._1, t._2)));
    }
  }

  public String getName() {
    return name;
  }

  public long getStartMs() {
    return startMs;
  }

  public long getDurationMs() {
    return durationMs;
  }

  public void setDurationMs(long durationMs) {
    this.durationMs = durationMs;
  }

  public void multiplyDurationMs(double deltaPercentage) {
    this.durationMs = (long) ((1.0 + deltaPercentage) * this.durationMs);
  }

  public String getSpanId() {
    return spanId;
  }

  public void setSpanId(String spanId) {
    this.spanId = spanId;
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public List<Map<String, String>> getAnnotations() {
    return annotations;
  }

  public void shiftStartMs(long deltaMs) {
    this.startMs += deltaMs;
  }

  /**
   * Conversion to span used by TraceLoader tool.
   *
   * @return Converted span.
   */
  public Span toSpan() {
    List<Pair<String, String>> tags = new LinkedList<>();
    annotations.forEach(a -> a.forEach((k, v) -> tags.add(new Pair<>(k, v))));

    List<UUID> parents = tags.stream().filter(pair -> pair._1.equals(PARENT)).
        map(id -> UUID.fromString(id._2)).collect(Collectors.toList());

    List<UUID> followsFrom = tags.stream().filter(pair -> pair._1.equals(FOLLOWS_FROM)).
        map(id -> UUID.fromString(id._2)).collect(Collectors.toList());

    return new Span(name, startMs, durationMs, host, UUID.fromString(traceId),
        UUID.fromString(spanId), parents, followsFrom, tags, null, SpanKind.REGULAR);
  }
}