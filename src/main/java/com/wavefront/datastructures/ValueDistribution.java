package com.wavefront.datastructures;

import java.util.Random;

/**
 * The class represents the value of any type of distribution that is needed to generate.
 *
 * @author Norayr Chaparyan (nchaparyan@vmware.com)
 */
@SuppressWarnings("unused")
public class ValueDistribution extends Distribution {
  private static final Random RANDOM = new Random(System.currentTimeMillis());
  /**
   * Start value of the bin values.
   */
  public int startValue;
  /**
   * End value of the bin values.
   */
  public int endValue;

  public ValueDistribution() {
  }

  public ValueDistribution(int startValue, int endValue, double percentage) {
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
   * Get random number from the distribution range [startValue, endValue].
   */
  public int getValue() {
    return startValue + RANDOM.nextInt(endValue - startValue + 1);
  }
}
