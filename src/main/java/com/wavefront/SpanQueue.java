package com.wavefront;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.concurrent.ThreadSafe;

/**
 * A thread-safe span queue where Generators add traces and from which Senders pick them up.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
@ThreadSafe
public class SpanQueue {
  private final LinkedList<Span> spanQueue = new LinkedList<>();
  private final AtomicInteger traceCount = new AtomicInteger(0);
  private final AtomicInteger spanCount = new AtomicInteger(0);

  public void addLast(Span e) {
    synchronized (spanQueue) {
      spanQueue.addLast(e);
    }
  }

  public void addTrace(List<List<Span>> trace) {
    if (trace == null) {
      return;
    }

    synchronized (spanQueue) {
      trace.forEach(spans -> {
        spanQueue.addAll(spans);
        spanCount.addAndGet(spans.size());
      });
    }
    traceCount.addAndGet(1);
  }

  public int getEnteredTraceCount() {
    return traceCount.get();
  }

  public int getEnteredSpanCount() {
    return spanCount.get();
  }

  public Span pollFirst() {
    Span span;
    synchronized (spanQueue) {
      span = spanQueue.pollFirst();
    }
    return span;
  }

  /**
   * Gets all generated spans and clear the span queue
   *
   * @return Generated spans
   */
  public List<Span> getReadySpans() {
    List<Span> spans;
    synchronized (spanQueue) {
      spans = List.copyOf(spanQueue);
      spanQueue.clear();
    }
    return spans;
  }

  public int size() {
    int size;
    synchronized (spanQueue) {
      size = spanQueue.size();
    }
    return size;
  }
}
