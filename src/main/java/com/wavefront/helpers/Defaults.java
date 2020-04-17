package com.wavefront.helpers;

import com.wavefront.datastructures.Distribution;
import com.wavefront.datastructures.TagVariation;
import com.wavefront.sdk.common.Pair;

import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Default values for some input parameters.
 *
 * @author Sirak Ghazaryan (sghazaryan@vmware.com)
 */
public class Defaults {
  private static final Random RANDOM = new Random(System.currentTimeMillis());
  public static final String SERVICE = "service";
  public static final String ERROR = "error";
  public static final String DEBUG = "debug";
  public static final String DEFAULT_TYPE_NAME_PREFIX = "traceType_";
  public static final String DEFAULT_SPAN_NAME_SUFFIX = "abcdefghijklmnopqrstuvxyz";
  public static final int DEFAULT_NESTING_LEVEL = RANDOM.nextInt(6) + 4;
  public static final Pair<String, String> ERROR_TAG = new Pair<>(ERROR, "true");
  public static final Pair<String, String> DEBUG_TAG = new Pair<>(DEBUG, "true");

  public static final List<TagVariation> DEFAULT_MANDATORY_TAGS = List.of(
      new TagVariation("application", Set.of("Application_1")),
      new TagVariation("cluster", Set.of("us-west")),
      new TagVariation("shard", Set.of("primary")),
      new TagVariation(SERVICE, Set.of("Service_1", "Service_2")));

  public static final List<Distribution> DEFAULT_TRACE_DURATIONS = List.of(
      new Distribution(200, RANDOM.nextInt(500) + 200, 100));

  public static final List<Distribution> DEFAULT_SPANS_DISTRIBUTIONS = List.of(
      new Distribution(3, RANDOM.nextInt(10) + 5, 100));

}
