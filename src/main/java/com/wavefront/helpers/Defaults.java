package com.wavefront.helpers;

import com.wavefront.TraceTypePattern;
import com.wavefront.TraceTypePattern.Distribution;
import com.wavefront.TraceTypePattern.TagVariation;

import java.util.List;
import java.util.Random;

/**
 * Default values for some input parameters.
 *
 * @author Sirak Ghazaryan (sghazaryan@vmware.com)
 */
public class Defaults {
  private static final Random RANDOM = new Random(System.currentTimeMillis());

  public static final List<TraceTypePattern.TagVariation> DEFAULT_MANDATORY_TAGS = List.of(
      new TagVariation("application", List.of("Application_1")),
      new TagVariation("cluster", List.of("us-west")),
      new TagVariation("shard", List.of("primary")),
      new TagVariation("service", List.of("Service_1", "Service_2")));

  public static final List<Distribution> DEFAULT_TRACE_DURATIONS = List.of(
      new Distribution(200, RANDOM.nextInt(500) + 200, 100));

  public static final List<Distribution> DEFAULT_SPANS_DISTRIBUTIONS = List.of(
      new Distribution(3, RANDOM.nextInt(10) + 5, 100));

  public static final int DEFAULT_NESTING_LEVEL = RANDOM.nextInt(6) + 4;

  public static final String DEFAULT_TYPE_NAME_PREFIX = "traceType_";

  public static final String DEFAULT_SPAN_NAME_SUFFIX = "abcdefghijklmnopqrstuvxyz";
}
