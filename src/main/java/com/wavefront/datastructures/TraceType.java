package com.wavefront.datastructures;

import java.util.List;

/**
 * A full-signature traceType definition.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public class TraceType {
  /**
   * Percentage of traces of the given type.
   */
  public double tracePercentage;
  /**
   * Number of spans in the trace.
   */
  public int spansCount;
  /**
   * Erroneous traces percentage. If it is not -1 it will hide errorConditions.
   */
  public double errorRate = -1;
  /**
   * Debug traces percentage.
   */
  public double debugRate = -1;
  /**
   * Distribution of durations for the given trace type.
   */
  public List<ValueDistribution> traceDurations;
  /**
   * An iterator for working with trace durations.
   */
  public DistributionIterator<ValueDistribution> traceDurationsIterator;
  /**
   * Conditions for marking trace and span as erroneous.
   */
  public List<ErrorCondition> errorConditions;

  /**
   * Initialize distribution iterator(random or exact mode).
   */
  public void init(int traceCount) {
    // If the total number of traces is specified, then this is the exact mode,
    // otherwise it is the random mode.
    if (traceCount > 0) {
      this.traceDurationsIterator = new ExactDistributionIterator<>(this.traceDurations, traceCount);
    } else {
      this.traceDurationsIterator = new RandomDistributionIterator<>(this.traceDurations);
    }
  }

  public ValueDistribution getNextTraceDuration() {
    return traceDurationsIterator.getNextDistribution();
  }
}