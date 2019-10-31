package com.wavefront;

import com.wavefront.sdk.common.Pair;
import com.wavefront.sdk.common.Utils;
import com.wavefront.sdk.entities.tracing.SpanLog;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * TODO
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

  public Span(String _name, long _startMillis, long _durationMillis, @Nullable String _source,
              UUID _traceUUID, UUID _spanUUID, @Nullable List<UUID> _parents, @Nullable List<UUID> _followsFrom,
              @Nullable List<Pair<String, String>> _tags, @Nullable List<SpanLog> _spanLogs) {
    name = _name;
    startMillis = _startMillis;
    durationMillis = _durationMillis;
    source = _source;
    traceUUID = _traceUUID;
    spanUUID = _spanUUID;
    parents = _parents;
    followsFrom = _followsFrom;
    tags = _tags;
    spanLogs = _spanLogs;
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
}