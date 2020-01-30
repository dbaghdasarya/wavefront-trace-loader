package com.wavefront;

import com.google.common.base.Strings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.wavefront.helpers.Defaults;
import com.wavefront.helpers.TraceTypePatternSanitizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(converter = TraceTypePatternSanitizer.class)
@SuppressWarnings("unused")
public class TraceTypePattern {
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

  /**
   * Class represents distribution need for generation of spans and durations per trace type.
   */
  public static class Distribution {
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    /**
     * Start value of the bin values.
     */
    public int startValue;
    /**
     * End value of the bin values.
     */
    public int endValue;
    /**
     * Occurrence of the values from the interval startValue, endValue in percents.
     */
    public int percentage;

    public Distribution() {

    }

    public Distribution(int startValue, int endValue, int percentage) {
      this.startValue = startValue;
      this.endValue = endValue;
      this.percentage = percentage;
    }

    @Override
    public String toString() {
      return "Distribution {" +
          "startValue = " + startValue +
          ", endValue = " + endValue +
          ", percentage = " + percentage +
          '}';
    }

    /**
     * Get random number from the distribution range [startValue, endValue]
     */
    public int getValue() {
      return startValue + RANDOM.nextInt(endValue - startValue + 1);
    }
  }

  /**
   * Class represents a tag name and variation of its values
   */
  public static class TagVariation {
    /**
     * Name of the tag
     */
    public String tagName;

    /**
     * Possible values of the tag
     */
    @Nonnull
    public List<String> tagValues;

    public TagVariation(@Nonnull String tagName,
                        @Nonnull List<String> tagValues) {
      this.tagName = tagName;
      this.tagValues = tagValues;
    }

    public TagVariation() {
      this.tagValues = new ArrayList<>();
    }
  }

  /**
   * Class represents condition on which error tag should be set for a span
   */
  public static class ErrorCondition {
    /**
     * Span Names to which the error conditions should be applied. If null, will not be taken into
     * account.
     */
    public List<String> spanNames;
    /**
     * Name of a tag which is route cause of the error
     */
    public String tagName;

    /**
     * Value of a tag which is a route cause of the error
     */
    public String tagValue;

    /**
     * Errors rate for the given tag-value pair
     */
    public int errorRate;

    public ErrorCondition() {
    }

    public ErrorCondition(List<String> spanNames,
                          @Nonnull String tagName,
                          @Nonnull String tagValue,
                          int errorRate) {
      this.spanNames = spanNames;
      this.tagName = tagName;
      this.tagValue = tagValue;
      this.errorRate = errorRate;
    }
  }
}