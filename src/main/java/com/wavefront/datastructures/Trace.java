package com.wavefront.datastructures;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import com.wavefront.sdk.common.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Data structure representing a trace.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public class Trace {
  private static final Random RANDOM = new Random(System.currentTimeMillis());

  private final List<List<Span>> spans;
  private final int levels;
  private int spansCount = 0;
  private int debugSpansCount = 0;
  private boolean error = false;
  private String traceId;
  private long startMs = Long.MAX_VALUE;
  private long endMs = Long.MIN_VALUE;

  public Trace(int levels) {
    this.levels = levels;
    spans = Lists.newArrayListWithExpectedSize(levels);
    for (int n = 0; n < levels; n++) {
      spans.add(new LinkedList<>());
    }
  }

  /**
   * Add a span on the mentioned level.
   *
   * @param level Level number for adding (root is 0).
   * @param span  Span to be added.
   */
  public void add(int level, Span span) {
    spans.get(level).add(span);
    if (Strings.isNullOrEmpty(this.traceId)) {
      this.traceId = span.getTraceUUID().toString();
    }
    setStartMs(span.getStartMillis());
    setEndMs(span.getDuration() + span.getStartMillis());
    spansCount++;
    if (span.getTags() != null) {
      if (!error && span.getTags().contains(new Pair<>("error", "true"))) {
        error = true;
      }
      if (span.getTags().contains(new Pair<>("debug", "true"))) {
        debugSpansCount++;
      }
    }
  }

  /**
   * Randomly creates parent-children connections between levels.
   */
  public void createRandomConnections() {
    int upperLevelSize;
    for (int n = levels - 1; n > 0; n--) {
      upperLevelSize = spans.get(n - 1).size();
      for (Span childSpan : spans.get(n)) {
        childSpan.addParent(spans.get(n - 1).get(RANDOM.nextInt(upperLevelSize)));
      }
    }
  }

  public int getSpansCount() {
    return spansCount;
  }

  public boolean isError() {
    return error;
  }

  public int getDebugSpansCount() {
    return debugSpansCount;
  }

  public final List<List<Span>> getSpans() {
    return spans;
  }

  public boolean setStartMs(long startMs) {
    if (this.startMs > startMs) {
      this.startMs = startMs;
      return true;
    }
    return false;
  }

  public boolean setEndMs(long endMs) {
    if (this.endMs < endMs) {
      this.endMs = endMs;
      return true;
    }
    return false;
  }

  /**
   * Converting to format compatible with traces exported from the Wavefront GUI.
   *
   * @return Converted trace.
   */
  public TraceFromWF toWFTrace() {
    TraceFromWF wfTrace = new TraceFromWF();
    wfTrace.setSpans(getSpans().stream().flatMap(List::stream).map(Span::toWFSpan).
        collect(Collectors.toList()));

    return wfTrace;
  }
}