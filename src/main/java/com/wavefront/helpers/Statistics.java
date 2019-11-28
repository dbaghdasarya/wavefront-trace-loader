package com.wavefront.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wavefront.Span;
import com.wavefront.sdk.common.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class collects statistics about generated traces such as traces count, spans count and trace
 * duration per trace type.
 *
 * @author Sirak Ghazaryan(sghazaryan@vmware.com)
 */
public class Statistics {
  private final Map<String, TypeStatistic> tracesByType = new HashMap<>();
  private int tracesSum = 0;
  private int errorsSum = 0;
  private int debugSpansSum = 0;

  public void offer(String traceTypeName, List<List<Span>> trace, int traceDuration) {
    tracesSum++;

    boolean error = false;
    int debugSpansCount = 0;
    for (int i = 0; i < trace.size(); i++) {
      for (int j = 0; j < trace.get(i).size(); j++) {
        Span span = trace.get(i).get(j);
        if (span.getTags() != null) {
          if (span.getTags().contains(new Pair<>("error", "true"))) {
            error = true;
          }
          if (span.getTags().contains(new Pair<>("debug", "true"))) {
            debugSpansCount++;
          }
        }
      }
    }
    if (error) {
      errorsSum++;
    }
    debugSpansSum += debugSpansCount;
    int spansCount = trace.stream().flatMap(List::stream).collect(Collectors.toList()).size();

    tracesByType.putIfAbsent(traceTypeName, new TypeStatistic());
    TypeStatistic tobeUpdated = tracesByType.get(traceTypeName);
    tobeUpdated.update(spansCount, traceDuration, error, debugSpansCount);
    tracesByType.put(traceTypeName, tobeUpdated);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Total traces: ").append(tracesSum).append('\n');
    sb.append("Total errors: ").append(errorsSum).append('\n');
    sb.append("Total errors percentage: ").append(Math.round((double) errorsSum / tracesSum * 100)).append('\n');
    sb.append("Total debug spans: ").append(debugSpansSum).append('\n');
    tracesByType.forEach((k, v) -> {
      sb.append("\nType: ").append(k).append('\n');
      sb.append("Count ").append(v.count).append('\n');
      sb.append("Percentage ").
          append(Math.round((double) v.count / tracesSum * 100)).append('\n');
      sb.append("Spans mean ").append(Math.round((double) v.spansSum / v.count)).append('\n');
      sb.append("Spans min ").append(v.spansMin).append('\n');
      sb.append("Spans max ").append(v.spansMax).append('\n');
      sb.append("Trace duration mean ").
          append(Math.round((double) v.traceDuration / v.count)).append('\n');
      sb.append("Trace duration min ").append(v.traceDurationMin).append('\n');
      sb.append("Trace duration max ").append(v.traceDurationMax).append('\n');
      sb.append("Errors count ").append(v.errorCount).append('\n');
      sb.append("Errors percentage ").
          append(Math.round((double) v.errorCount / errorsSum * 100)).append('\n');
      sb.append("Debug spans count ").append(v.debugSpansCount).append('\n');
    });
    return sb.toString();
  }

  public String toJSONString() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode rootNode = mapper.createObjectNode();
    rootNode.put("Total traces", tracesSum);
    rootNode.put("Total errors", errorsSum);
    rootNode.put("Total errors percentage", Math.round((double) errorsSum / tracesSum * 100));
    rootNode.put("Total debug spans ", debugSpansSum);
    tracesByType.forEach((k, v) -> {
      ObjectNode node = mapper.createObjectNode();
      node.put("Count", v.count);
      node.put("Percentage", Math.round((double) v.count / tracesSum * 100));
      node.put("Spans mean", Math.round((double) v.spansSum / v.count));
      node.put("Spans min", v.spansMin);
      node.put("Spans max", v.spansMax);
      node.put("Trace duration mean", Math.round((double) v.traceDuration / v.count));
      node.put("Trace duration min", v.traceDurationMin);
      node.put("Trace duration max", v.traceDurationMax);
      node.put("Errors count", v.errorCount);
      node.put("Errors percentage", Math.round((double) v.errorCount / errorsSum * 100));
      node.put("Debug spans count ", v.debugSpansCount);
      rootNode.set(k, node);
    });
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
  }


  private static class TypeStatistic {
    int count = 0;
    int errorCount = 0;
    int debugSpansCount = 0;
    int spansSum = 0;
    int spansMin = Integer.MAX_VALUE;
    int spansMax = Integer.MIN_VALUE;
    int traceDuration = 0;
    int traceDurationMin = Integer.MAX_VALUE;
    int traceDurationMax = Integer.MIN_VALUE;

    void update(int spansCount, int duration, boolean error, int debugSpansCount) {
      if (error) {
        errorCount++;
      }
      this.debugSpansCount += debugSpansCount;
      count++;
      spansSum += spansCount;
      spansMin = Math.min(spansMin, spansCount);
      spansMax = Math.max(spansMax, spansCount);

      traceDuration += duration;
      traceDurationMin = Math.min(traceDurationMin, duration);
      traceDurationMax = Math.max(traceDurationMax, duration);
    }
  }
}
