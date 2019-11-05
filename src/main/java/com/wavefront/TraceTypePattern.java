package com.wavefront;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
  public int nestingLevel;
  public int tracePercentage;
  public int errorRate;
  public List<Distribution> spansDistributions;
  public List<Distribution> traceDurations;
  public List<TagVariation> mandatoryTags;
  public List<TagVariation> optionalTags;
  public int optionalTagsPercentage = 100;

  public TraceTypePattern(String traceTypeName, int nestingLevel, int tracePercentage,
                          List<Distribution> spansDistributions, List<Distribution> traceDurations,
                          List<TagVariation> mandatoryTags, List<TagVariation> optionalTags,
                          int errorRate) {
    this.traceTypeName = traceTypeName;
    this.nestingLevel = nestingLevel;
    this.tracePercentage = tracePercentage;
    this.spansDistributions = spansDistributions;
    this.traceDurations = traceDurations;
    this.mandatoryTags = mandatoryTags;
    this.optionalTags = optionalTags;
    this.errorRate = errorRate;
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
}

