package com.wavefront.datastructures;

import java.util.Random;

/**
 * Class represents distribution need for generation of spans and durations per trace type.
 *
 * @author Sirak Ghazaryan (sghazaryan@vmware.com)
 */
public class Distribution {
  private static final Random RANDOM = new Random(System.currentTimeMillis());
  public static final double HUNDRED_PERCENT = 100;
  /**
   * Start value of the bin values.
   */
  public int startValue;
  /**
   * End value of the bin values.
   */
  public int endValue;
  /**
   * Portion of the values from the interval startValue, endValue.
   */
  public double portion;
  /**
   * Occurrence of the values from the interval startValue, endValue in percents.
   */
  public double percentage;

  public Distribution() {

  }

  public Distribution(int startValue, int endValue, double percentage) {
    this.startValue = startValue;
    this.endValue = endValue;
    this.percentage = percentage;
    this.portion = 0;
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
