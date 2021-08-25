package com.wavefront.helpers;

import com.wavefront.datastructures.TagVariation;
import com.wavefront.datastructures.ValueDistribution;
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
  public static final String APPLICATION = "application";
  public static final String CLUSTER = "cluster";
  public static final String SHARD = "shard";
  public static final String PARENT = "parent";
  public static final String FOLLOWS_FROM = "followsFrom";
  public static final String DEFAULT_TYPE_NAME_PREFIX = "traceType_";
  public static final String DEFAULT_SPAN_NAME_SUFFIX = "abcdefghijklmnopqrstuvxyz";
  public static final String PATTERN = "PATTERN";
  public static final String TOPOLOGY = "TOPOLOGY";
  public static final int DEFAULT_NESTING_LEVEL = RANDOM.nextInt(6) + 4;
  public static final Pair<String, String> ERROR_TAG = new Pair<>(ERROR, "true");
  public static final Pair<String, String> DEBUG_TAG = new Pair<>(DEBUG, "true");
  public static final double HUNDRED_PERCENT = 100;

  public static final List<TagVariation> DEFAULT_MANDATORY_TAGS = List.of(
      new TagVariation(APPLICATION, Set.of("Application_1")),
      new TagVariation(CLUSTER, Set.of("us-west")),
      new TagVariation(SHARD, Set.of("primary")),
      new TagVariation(SERVICE, Set.of("Service_1", "Service_2")));

  public static final List<ValueDistribution> DEFAULT_TRACE_DURATIONS = List.of(
      new ValueDistribution(200, RANDOM.nextInt(500) + 200, 100));

  public static final List<ValueDistribution> DEFAULT_SPANS_DISTRIBUTIONS = List.of(
      new ValueDistribution(3, RANDOM.nextInt(10) + 5, 100));
  public static final List<String> STAT_ROOT_NAME_LIST = List.of(
      "PATTERN_STAT",
      "TOPOLOGY_STAT"
  );

}
