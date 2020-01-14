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
public class DataQueue {
  private final LinkedList<Span> spanQueue = new LinkedList<>();
  private final LinkedList<Trace> traceQueue;
  private final AtomicInteger traceCount = new AtomicInteger(0);
  private final AtomicInteger spanCount = new AtomicInteger(0);
  private boolean keepTraces = false;

  DataQueue(boolean keepTraces) {
    this.keepTraces = keepTraces;
    if (this.keepTraces) {
      traceQueue = new LinkedList<>();
    } else {
      traceQueue = null;
    }
  }

  public void addTrace(Trace trace) {
    if (trace == null) {
      return;
    }

    synchronized (spanQueue) {
      if (keepTraces) {
        traceQueue.add(trace);
      }
      trace.getSpans().forEach(spans -> {
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

  public Span pollFirstSpan() {
    synchronized (spanQueue) {
      return spanQueue.pollFirst();
    }
  }

  public Trace pollFirstTrace() {
    if (!keepTraces) {
      return null;
    }
    // Trace dump perfromed only in case saving to file and no need in synchronization.
    return traceQueue.pollFirst();
  }

  /**
   * Gets all generated spans and clear the span queue
   *
   * @return Generated spans
   */
  public List<Span> getReadySpans() {
    synchronized (spanQueue) {
      List<Span> spans = List.copyOf(spanQueue);
      spanQueue.clear();
      return spans;
    }
  }

  public int size() {
    synchronized (spanQueue) {
      return spanQueue.size();
    }
  }
}
