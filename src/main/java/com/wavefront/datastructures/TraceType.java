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
  public int errorRate = -1;
  /**
   * Debug traces percentage.
   */
  public int debugRate = -1;
  /**
   * Distribution of durations for the given trace type.
   */
  public List<Distribution> traceDurations;
  /**
   * Conditions for marking trace and span as erroneous.
   */
  public List<ErrorCondition> errorConditions;
}