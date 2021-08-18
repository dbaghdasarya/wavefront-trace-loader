package com.wavefront;

import com.wavefront.datastructures.Span;
import com.wavefront.datastructures.Trace;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.concurrent.ThreadSafe;

/**
 * A thread-safe data queue where Generators add traces/spans and from which Senders pick them up.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
@ThreadSafe
public class DataQueue {
  private final LinkedList<Span> spanQueue = new LinkedList<>();
  private final LinkedList<Trace> traceQueue = new LinkedList<>();
  private final AtomicInteger traceCount = new AtomicInteger(0);
  private final AtomicInteger spanCount = new AtomicInteger(0);
  private boolean keepTraces;

  /**
   * DataQueue constructor.
   *
   * @param keepTraces If true, traces will be stored for saving to file.
   */
  DataQueue(boolean keepTraces) {
    this.keepTraces = keepTraces;
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
    // Trace dump performed only in case saving to file and no need in synchronization.
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
