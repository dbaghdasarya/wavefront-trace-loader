package com.wavefront.datastructures;

import com.google.common.base.Strings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.wavefront.helpers.Defaults;
import com.wavefront.helpers.TraceTypePatternSanitizer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data structure representing trace type loaded from the pattern.json file.
 *
 * @author Sirak Ghazaryan (sghazaryan@vmware.com), Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(converter = TraceTypePatternSanitizer.class)
@SuppressWarnings("unused")
public class TraceTypePattern {
  public final Map<String, Set<String>> serviceMap = new LinkedHashMap<>();
  public String traceTypeName;
  public String spanNameSuffixes;
  public int nestingLevel;
  public int tracePercentage;
  public int errorRate;
  public int debugRate;
  public List<Distribution> traceDurations;
  public List<Distribution> spansDistributions;
  public List<Distribution> spansDurations;
  public List<TagVariation> mandatoryTags;
  public List<TagVariation> optionalTags;
  public int optionalTagsPercentage = 100;
  public List<ErrorCondition> errorConditions;
  public Set<String> rootLevelServices;

  public TraceTypePattern(String traceTypeName, String spanNameSuffixes,
                          int nestingLevel, int tracePercentage,
                          List<Distribution> spansDistributions, List<Distribution> traceDurations,
                          List<TagVariation> mandatoryTags, int errorRate, int debugRate) {
    this.traceTypeName = traceTypeName;
    if (Strings.isNullOrEmpty(spanNameSuffixes)) {
      this.spanNameSuffixes = Defaults.DEFAULT_SPAN_NAME_SUFFIX;
    } else {
      this.spanNameSuffixes = spanNameSuffixes;
    }
    this.nestingLevel = nestingLevel;
    this.tracePercentage = tracePercentage;
    this.spansDistributions = spansDistributions;
    this.spansDurations = new ArrayList<>();
    this.traceDurations = traceDurations;
    this.mandatoryTags = mandatoryTags;
    this.errorRate = errorRate;
    this.debugRate = debugRate;
  }

  public TraceTypePattern() {
  }
}