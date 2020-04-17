package com.wavefront.topology;

import com.wavefront.datastructures.TagVariation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Combines all service-related data. For internal use of package classes.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
class ServiceInfo {
  final Set<String> children = new HashSet<>();
  final Set<String> parents = new HashSet<>();
  final Map<String, TagVariation> tags = new HashMap<>();
  int spansNumber = 1;
}