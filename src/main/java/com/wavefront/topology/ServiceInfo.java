package com.wavefront.topology;

import com.wavefront.datastructures.TagVariation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Combines all service-related data. For internal use of package classes.
 * TraceTypeSanitizer reads service related data from different parts of the topology.json file,
 * process it and bring together in a manner that is most suitable for further usage.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
class ServiceInfo {
  /**
   * List of services that can follow the given service.
   */
  final Set<String> children = new HashSet<>();
  /**
   * List of services that can be a parent for the given service.
   */
  final Set<String> parents = new HashSet<>();
  /**
   * Set of possible tags for the given service.
   */
  final Map<String, TagVariation> tags = new HashMap<>();
  /**
   * Number of operations variations for the given service.
   */
  int spansNumber = 1;
}