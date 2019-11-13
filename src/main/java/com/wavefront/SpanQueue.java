package com.wavefront;

import java.util.LinkedList;
import java.util.List;

/**
 * TODO
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public class SpanQueue {
  private LinkedList<Span> spanQueue = new LinkedList<>();
  private int traceCount = 0;

  public void addLast(Span e) {
    spanQueue.addLast(e);
  }

  public void addTrace(List<List<Span>> trace) {
    if (trace == null) {
      return;
    }

    trace.forEach(spans -> {
      spanQueue.addAll(spans);
    });
    traceCount++;
  }

  public int getTraceCount() {
    return traceCount;
  }

  public Span pollFirst() {
    return spanQueue.pollFirst();
  }

  public void clear() {
    spanQueue.clear();
  }

  public int size() {
    return spanQueue.size();
  }
}
