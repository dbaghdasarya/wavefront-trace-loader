package com.wavefront.topology;

import com.wavefront.datastructures.Distribution;
import com.wavefront.datastructures.ErrorCondition;

import java.util.List;
import java.util.Random;

/**
 * A full-signature traceType.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public class TraceType {
  private static final Random RANDOM = new Random(System.currentTimeMillis());

  public int tracePercentage;
  public int spansCount;
  public int errorRate = -1;
  public int debugRate = -1;
  public List<Distribution> traceDurations;
  public List<ErrorCondition> errorConditions;
}