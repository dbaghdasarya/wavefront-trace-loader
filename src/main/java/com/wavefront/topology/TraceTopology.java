package com.wavefront.topology;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.wavefront.datastructures.TagVariation;
import com.wavefront.datastructures.TraceType;
import com.wavefront.sdk.common.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.wavefront.datastructures.Distribution.HUNDRED_PERCENT;
import static com.wavefront.helpers.WftlUtils.getRandomFromSet;

/**
 * Data structure representing trace topology loaded from the topology.json file.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(converter = TraceTopologySanitizer.class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class TraceTopology {
  private static final Random RANDOM = new Random(System.currentTimeMillis());
  /**
   * Trace types definitions read from the topology.json.
   */
  public List<TraceType> traceTypes;
  List<ServiceTags> serviceTags;
  List<ServiceSpansNumber> serviceSpansNumbers;
  List<ServiceConnection> serviceConnections;
  Set<String> rootLevelServices;
  Map<String, ServiceInfo> serviceInfos = new HashMap<>();

  /**
   * Randomly selects service eligible for using as service of the root span.
   *
   * @return Service name.
   */
  @Nullable
  public String getRandomRootService() {
    if (rootLevelServices == null || rootLevelServices.isEmpty()) {
      return null;
    }
    return getRandomFromSet(rootLevelServices);
  }

  /**
   * Generates span name for the given service in format "serviceName_00n", n is span number set in
   * topology.json for the given service. Default value of the span number is 1.
   *
   * @param service Service name.
   * @return Generated spanName.
   */
  @Nullable
  public String getSpanName(@Nonnull String service) {
    ServiceInfo serviceInfo = serviceInfos.get(service);
    if (serviceInfo == null) {
      return null;
    }

    return String.format("%s_%03d", service, RANDOM.nextInt(serviceInfo.spansNumber) + 1);
  }

  /**
   * Randomly selects service name from the lists of children of previous level services, based on
   * serviceConnection of topology.json.
   *
   * @param previousLevelServices List of previous level services.
   * @return Randomly selected service name.
   */
  @Nullable
  public String getNextLevelService(Set<String> previousLevelServices) {
    if (previousLevelServices == null || previousLevelServices.isEmpty()) {
      return null;
    }

    ServiceInfo serviceInfo = null;

    // Try to get random child service.
    int maxAttempts = previousLevelServices.size();
    while (serviceInfo == null && maxAttempts >= 0) {
      serviceInfo = serviceInfos.get(getRandomFromSet(previousLevelServices));
      if (serviceInfo.children.isEmpty()) {
        serviceInfo = null;
      }
      maxAttempts--;
    }

    // If random getting failed iterate whole set and get the first.
    if (serviceInfo == null) {
      final Iterator<String> it = previousLevelServices.iterator();
      while (it.hasNext() && serviceInfo == null) {
        serviceInfo = serviceInfos.get(it.next());
        if (serviceInfo.children.isEmpty()) {
          serviceInfo = null;
        }
      }
    }

    if (serviceInfo == null) {
      return null;
    } else {
      return getRandomFromSet(serviceInfo.children);
    }
  }

  /**
   * An instance of tags and their values set for the given service (based on topology.json
   * definition).
   *
   * @param service Service name.
   * @return Set of tags and their values.
   */
  @Nullable
  public List<Pair<String, String>> getServiceTags(@Nonnull String service) {
    List<Pair<String, String>> tags = new LinkedList<>();
    ServiceInfo serviceInfo = serviceInfos.get(service);
    if (serviceInfo == null) {
      return null;
    }

    serviceInfo.tags.forEach((k, v) -> {
      // Values in range [0..100] inclusive are possible.
      if (RANDOM.nextInt(HUNDRED_PERCENT) + 1 <= v.percentage) {
        tags.add(new Pair<>(k, v.getRandomValue()));
      }
    });

    return tags;
  }

  /**
   * Tests that the given some service is parent of another (based on topology.json definition).
   *
   * @param child  Child service name.
   * @param parent Parent service name.
   * @return True if given parent is really parent of a child service.
   */
  public boolean isParent(String child, String parent) {
    final ServiceInfo serviceInfo = serviceInfos.get(child);
    if (serviceInfo != null) {
      return serviceInfo.parents.contains(parent);
    }
    return false;
  }

  /**
   * Set of services and tags relations loaded from the topology.json.
   */
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  static class ServiceTags {
    Set<String> services;
    List<TagVariation> mandatoryTags;
    List<TagVariation> optionalTags;
    int optionalTagsPercentage = 100;
  }

  /**
   * Set of services and number of operations of services loaded from the topology.json.
   */
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  static class ServiceSpansNumber {
    Set<String> services;
    int spansNumber;
  }

  /**
   * Class represents ancestor-descendant relation between services
   */
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  static class ServiceConnection {
    boolean root = false;
    /**
     * Ancestor services
     */
    final Set<String> services = new HashSet<>();
    /**
     * Descendant services
     */
    final Set<String> children = new HashSet<>();
  }
}