package com.wavefront.datastructures;

import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;

/**
 * Class represents distribution need for generation of spans and durations per trace type.
 *
 * @author Sirak Ghazaryan (sghazaryan@vmware.com)
 */
public class Distribution {
  private static final Random RANDOM = new Random(System.currentTimeMillis());
  public static final int HUNDRED_PERCENT = 100;
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
   * Get random number from the distribution range [startValue, endValue].
   */
  public int getValue() {
    return startValue + RANDOM.nextInt(endValue - startValue + 1);
  }

  /**
   * Normalize set of distributions.
   *
   * @param distributions Distributions to be normalized.
   */
  public static void normalizeCanonicalDistributions(@Nonnull List<Distribution> distributions) {
    double percentsSum = distributions.stream().mapToDouble(d -> d.percentage).sum();
    // Don't do anything if input values are already normalized.
    if (Double.compare(percentsSum, HUNDRED_PERCENT) != 0) {
      double ratio = (double) HUNDRED_PERCENT / percentsSum;
      distributions.forEach(d ->
          d.percentage = (int) Math.round(d.percentage * ratio));
    }
  }

  /**
   * Generic helper method which calculate next item based on distribution percentages using
   * randomization.
   *
   * @param percentages List of distribution percentages.
   * @return Return an index of the next item.
   */
  public static int getIndexOfNextItem(@Nonnull List<Integer> percentages) {
    final int randomPercent = RANDOM.nextInt(HUNDRED_PERCENT) + 1;
    int left = 0;
    for (int i = 0; i < percentages.size(); i++) {
      if (randomPercent > left && randomPercent <= percentages.get(i) + left) {
        return i;
      }
      left += percentages.get(i);
    }
    return 0;
  }
}
