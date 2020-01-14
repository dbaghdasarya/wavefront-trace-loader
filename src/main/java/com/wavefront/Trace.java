package com.wavefront;

import com.google.common.collect.Lists;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wavefront.sdk.common.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Trace {
  private static final Random RANDOM = new Random(System.currentTimeMillis());
  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

  private final List<List<Span>> spans;
  private final int levels;
  private int spansCount = 0;
  private int debugSpansCount = 0;
  private boolean error = false;

  public Trace(int levels) {
    this.levels = levels;
    spans = Lists.newArrayListWithExpectedSize(levels);
    for (int n = 0; n < levels; n++) {
      spans.add(new LinkedList<>());
    }
  }

  /**
   * Add a span on the mentioned level.
   * @param level Level number for adding (root is 0).
   * @param span  Span to be added.
   */
  public void add(int level, Span span) {
    spans.get(level).add(span);
    spansCount++;
    if (!error && span.getTags().contains(new Pair<>("error", "true"))) {
      error = true;
    }
    if (span.getTags().contains(new Pair<>("debug", "true"))) {
      debugSpansCount++;
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

  public String toJSONString() throws Exception {
    return JSON_MAPPER.writeValueAsString(this) + "\n";
  }
}
