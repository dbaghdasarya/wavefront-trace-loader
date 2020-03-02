package com.wavefront.datastructures;

import com.wavefront.sdk.common.Pair;
import com.wavefront.sdk.common.Utils;
import com.wavefront.sdk.entities.tracing.SpanLog;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * Data structure representing a span.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public class Span {
  private String name;
  private long startMillis;
  private long durationMillis;
  @Nullable
  private String source;
  private UUID traceUUID;
  private final UUID spanUUID;
  @Nullable
  private List<UUID> parents;
  @Nullable
  private List<UUID> followsFrom;
  @Nullable
  private List<Pair<String, String>> tags;
  @Nullable
  private List<SpanLog> spanLogs;


  public Span() {
    spanUUID = UUID.randomUUID();
  }

  public Span(String name, long startMillis, long durationMillis, @Nullable String source,
              UUID traceUUID, UUID spanUUID, @Nullable List<UUID> parents,
              @Nullable List<UUID> followsFrom, @Nullable List<Pair<String, String>> tags,
              @Nullable List<SpanLog> spanLogs) {
    this.name = name;
    this.startMillis = startMillis;
    this.durationMillis = durationMillis;
    this.source = source;
    this.traceUUID = traceUUID;
    this.spanUUID = spanUUID;
    this.parents = parents;
    this.followsFrom = followsFrom;
    this.tags = tags;
    this.spanLogs = spanLogs;
  }

  public UUID getSpanUUID() {
    return spanUUID;
  }

  public UUID getTraceUUID() {
    return traceUUID;
  }

  public long getDuration() {
    return durationMillis;
  }

  public long getStartMillis() {
    return startMillis;
  }

  public String getName() {
    return name;
  }

  @Nullable
  public String getSource() {
    return source;
  }

  @Nullable
  public List<UUID> getParents() {
    return parents;
  }

  @Nullable
  public List<UUID> getFollowsFrom() {
    return followsFrom;
  }

  @Nullable
  public List<Pair<String, String>> getTags() {
    return tags;
  }

  @Nullable
  public List<SpanLog> getSpanLogs() {
    return spanLogs;
  }

  public void addParent(Span parent) {
    // here we don't touch traceUUID of the span, because what to do when parent removed after?
    if (parents == null) {
      parents = new LinkedList<>();
    }
    parents.add(parent.getSpanUUID());
  }

  public boolean removeParent(Span parent) {
    if (parents == null) {
      return false;
    }
    parents.remove(parent.getSpanUUID());
    return true;
  }

  public String toString() {
    return Utils.tracingSpanToLineData(getName(), getStartMillis(), getDuration(),
        getSource(), getTraceUUID(), getSpanUUID(), getParents(), getFollowsFrom(),
        getTags(), getSpanLogs(), getSource());
  }

  /**
   * Convert to traces compatible with format of traces dumped from Wavefront.
   * @return Trace in format of Wavefront trace.
   */
  public SpanFromWF toWFSpan() {
    SpanFromWF wfSpan = new SpanFromWF();
    wfSpan.name = this.name;
    wfSpan.host = this.source;
    wfSpan.startMs = this.startMillis;
    wfSpan.durationMs = this.durationMillis;
    wfSpan.spanId = this.spanUUID.toString();
    wfSpan.traceId = this.traceUUID.toString();
    wfSpan.annotations = new LinkedList<>();
    if (tags != null) {
      tags.forEach(t -> wfSpan.annotations.add(Map.of(t._1, t._2)));
    }
    return wfSpan;
  }
}