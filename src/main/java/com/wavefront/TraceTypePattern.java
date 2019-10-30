package com.wavefront;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Random;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("unused")
public class TraceTypePattern {
  public String traceTypeName;
  public int nestingLevel;
  public int tracePercentage;
  public int errorRate;
  public List<Distribution> spansDistributions;
//  public LinkedList<Pair<String, LinkedList<String>>> mandatoryTags;
//  public HashMap<String, LinkedList<String>> optionalTags;

  public TraceTypePattern(String traceTypeName, int nestingLevel, int tracePercentage,
                          List<Distribution> distributions, int errorRate) {
    this.traceTypeName = traceTypeName;
    this.nestingLevel = nestingLevel;
    this.tracePercentage = tracePercentage;
    this.spansDistributions = distributions;
    this.errorRate = errorRate;
  }

  public TraceTypePattern() {

  }

  /**
   * Class represents distribution need for generation of spans and durations per trace type.
   */
  public static class Distribution {
    private final Random random = new Random(System.currentTimeMillis());
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
     * Get random number from the range [startValue, endValue]
     */
    public int getValue() {
      return startValue + random.nextInt(endValue - startValue + 1);
    }
  }
}

