package com.wavefront.datastructures;

import com.google.common.base.Strings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.wavefront.helpers.Defaults;
import com.wavefront.helpers.TraceTypePatternValidator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data structure representing trace type loaded from the pattern.json file. Fields names should
 * match appropriate names in pattern.json file.
 *
 * @author Sirak Ghazaryan (sghazaryan@vmware.com), Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(converter = TraceTypePatternValidator.class)
@SuppressWarnings("unused")
public class TraceTypePattern {
  public final Map<String, Set<String>> serviceMap = new LinkedHashMap<>();
  public String traceTypeName;
  public String spanNameSuffixes;
  public int nestingLevel;
  public double tracePercentage;
  public double errorRate;
  public double debugRate;
  public List<ValueDistribution> traceDurations;
  public List<ValueDistribution> spansDistributions;
  public List<ValueDistribution> spansDurations;
  private DistributionIterator<ValueDistribution> traceDurationsIterator;
  private DistributionIterator<ValueDistribution> spansDistributionsIterator;
  private DistributionIterator<ValueDistribution> spansDurationsIterator;
  public List<TagVariation> mandatoryTags;
  public List<TagVariation> optionalTags;
  public double optionalTagsPercentage = 100;
  public List<ErrorCondition> errorConditions;
  public Set<String> rootLevelServices;

  public TraceTypePattern(String traceTypeName, String spanNameSuffixes,
                          int nestingLevel, double tracePercentage,
                          List<ValueDistribution> spansDistributions,
                          List<ValueDistribution> traceDurations,
                          List<TagVariation> mandatoryTags, double errorRate, double debugRate) {
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

  /**
   * Initialize distributions iterators(random or exact mode).
   */
  public void init(int traceCount) {
    // Iterator for span durations should always be in random mode.
    this.spansDurationsIterator = new RandomDistributionIterator<>(this.spansDurations);

    // If the total number of traces is specified, then this is the exact mode,
    // otherwise it is the random mode.
    if (traceCount > 0) {
      this.spansDistributionsIterator = new ExactDistributionIterator<>(this.spansDistributions,
          traceCount);
      this.traceDurationsIterator = new ExactDistributionIterator<>(this.traceDurations,
          traceCount);
    } else {
      this.spansDistributionsIterator = new RandomDistributionIterator<>(this.spansDistributions);
      this.traceDurationsIterator = new RandomDistributionIterator<>(this.traceDurations);
    }
  }

  public ValueDistribution getNextSpanDuration() {
    return spansDurationsIterator.getNextDistribution();
  }

  public ValueDistribution getNextSpanDistribution() {
    return spansDistributionsIterator.getNextDistribution();
  }

  public ValueDistribution getNextTraceDuration() {
    return traceDurationsIterator.getNextDistribution();
  }
}