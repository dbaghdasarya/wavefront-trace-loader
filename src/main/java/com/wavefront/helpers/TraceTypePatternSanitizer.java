package com.wavefront.helpers;

import com.google.common.base.Strings;

import com.fasterxml.jackson.databind.util.StdConverter;
import com.wavefront.SpanSender;
import com.wavefront.TraceTypePattern;
import com.wavefront.TraceTypePattern.TagVariation;

import java.util.ArrayList;
import java.util.logging.Logger;

import static com.wavefront.helpers.Defaults.DEFAULT_MANDATORY_TAGS;
import static com.wavefront.helpers.Defaults.DEFAULT_NESTING_LEVEL;
import static com.wavefront.helpers.Defaults.DEFAULT_SPANS_DISTRIBUTIONS;
import static com.wavefront.helpers.Defaults.DEFAULT_SPAN_NAME_SUFFIX;
import static com.wavefront.helpers.Defaults.DEFAULT_TRACE_DURATIONS;
import static com.wavefront.helpers.Defaults.DEFAULT_TYPE_NAME_PREFIX;

/**
 * Sanitizer class which converts incorrect input values to default ones.
 *
 * @author Davit Baghdasaryan (dbaghdasarya@vmware.com)
 */
public class TraceTypePatternSanitizer extends StdConverter<TraceTypePattern, TraceTypePattern> {
  private static final Logger LOGGER = Logger.getLogger(SpanSender.class.getCanonicalName());
  private static int currentTypeIndex = 1;

  @Override
  public TraceTypePattern convert(TraceTypePattern value) {
    // check trace type name
    if (Strings.isNullOrEmpty(value.traceTypeName)) {
      value.traceTypeName = DEFAULT_TYPE_NAME_PREFIX + currentTypeIndex++;
      LOGGER.warning("Incorrect value of traceTypeName was replaced by " + value.traceTypeName);
    }
    // check trace type name
    if (Strings.isNullOrEmpty(value.spanNameSuffixes)) {
      value.spanNameSuffixes = DEFAULT_SPAN_NAME_SUFFIX;
      LOGGER.warning(value.traceTypeName +
          ": Empty value of span name suffixes was replaced by " + value.spanNameSuffixes);
    }
    // check nesting level
    if (value.nestingLevel <= 0) {
      value.nestingLevel = DEFAULT_NESTING_LEVEL;
      LOGGER.warning(value.traceTypeName + ": Incorrect value for nestingLevel was replaced by " +
          value.nestingLevel);
    }

    // Check and fix the tags set
    // Mandatory Tags must exist in the set and must have values
    if (value.mandatoryTags == null) {
      value.mandatoryTags = new ArrayList<>();
    }

    DEFAULT_MANDATORY_TAGS.forEach(tagVariation -> {
          TagVariation tempTagVariation = value.mandatoryTags.stream().
              filter(givenTag -> givenTag.tagName.equals(tagVariation.tagName)).findFirst().
              orElseGet(() -> {
                value.mandatoryTags.add(tagVariation);
                LOGGER.warning(value.traceTypeName + ": Missing mandatory tag was added - " +
                    tagVariation.tagName);
                return tagVariation;
              });

          if (tempTagVariation.tagValues.isEmpty()) {
            tempTagVariation.tagValues.addAll(tagVariation.tagValues);
            LOGGER.warning(value.traceTypeName + ": Values for mandatory tag were added - " +
                tagVariation.tagName);
          }
        }
    );

    // Check and fix optionalTagsPercentage value
    if (value.optionalTagsPercentage < 0 || value.optionalTagsPercentage > 100) {
      value.optionalTagsPercentage = 100;
      LOGGER.warning(value.traceTypeName + ": Meaningless value of the optionalTagsPercentage" +
          " was replaced with 100%");
    }

    // check traceDurations and spansDurations. As traceDurations have priority over spansDurations
    // the spansDurations will be skipped if traceDurations is set
    if (value.traceDurations == null) {
      value.traceDurations = new ArrayList<>();
    }
    if (value.spansDurations == null) {
      value.spansDurations = new ArrayList<>();
    }
    if (value.traceDurations.isEmpty() && value.spansDurations.isEmpty()) {
      value.traceDurations.addAll(DEFAULT_TRACE_DURATIONS);
      LOGGER.warning(value.traceTypeName + ": Neither traceDurations nor spansDurations were " +
          "provided. traceDurations were defaulted to " + value.traceDurations.toString());
    }
    if (!value.traceDurations.isEmpty() && !value.spansDurations.isEmpty()) {
      LOGGER.warning(value.traceTypeName + ": spansDurations will be skipped, " +
          "as traceDurations is set.");
    }

    // spans distributions
    if (value.spansDistributions == null) {
      value.spansDistributions = new ArrayList<>();
    }
    if (value.spansDistributions.isEmpty()) {
      value.spansDistributions.addAll(DEFAULT_SPANS_DISTRIBUTIONS);
      LOGGER.warning(value.traceTypeName + ": spansDistributions were not provided. Defaulted to " +
          value.spansDistributions.toString());
    }

    // errorConditions
    if (value.errorConditions != null) {
      if (value.errorConditions.removeIf(condition -> condition.errorRate < 1 || condition.errorRate > 100)) {
        LOGGER.warning(value.traceTypeName + ": Some of the errorConditions were removed " +
            "because the errorRate was not in the [1..100] range!");
      }
      if (value.errorConditions.isEmpty()) {
        value.errorConditions = null;
        LOGGER.warning(value.traceTypeName + ": Empty errorConditions block was completely " +
            "removed and the common errorRate will be applied!");
      }
    }

    return value;
  }
}