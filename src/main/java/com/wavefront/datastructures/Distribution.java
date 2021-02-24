package com.wavefront.datastructures;

import java.util.LinkedList;
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
  protected static double sumOfPercentages = 0;
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
  public double percentage;

  public Distribution() {

  }

  public Distribution(int startValue, int endValue, double percentage) {
    this.startValue = startValue;
    this.endValue = endValue;
    this.percentage = percentage;
  }

  /**
   * By use percentages, generating percent ranges.
   */
  protected static List<Double> generatePercentRanges(@Nonnull List<Double> percentages) {
    List<Double> percentRanges = new LinkedList<>();
    sumOfPercentages = 0;
    for (int i = 0; i < percentages.size(); i++) {
      sumOfPercentages += percentages.get(i);
      percentRanges.add(sumOfPercentages);
    }
    return percentRanges;
  }

  /**
   * Sum of all percentages.
   */
  protected static double sumOfPercentRanges() {
    return sumOfPercentages;
  }

  /**
   * Used binary search algorithm to find index.
   */
  protected static int getIndex(@Nonnull List<Double> percentRanges, double randomPercent) {
    int firstIndex = 0;
    int lastIndex = percentRanges.size() - 1;
    int middleIndex = 0;

    while (firstIndex <= lastIndex) {
      middleIndex = firstIndex + (lastIndex - firstIndex) / 2;
      if (percentRanges.get(middleIndex) == randomPercent) {
        return middleIndex;
      }
      if (percentRanges.get(middleIndex) < randomPercent) {
        firstIndex = middleIndex + 1;
      } else {
        lastIndex = middleIndex - 1;
      }
    }
    return middleIndex;
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
  public static int getIndexOfNextItem(@Nonnull List<Double> percentages) {
    final List<Double> percentRanges = generatePercentRanges(percentages);
    final double randomPercent = sumOfPercentRanges() * RANDOM.nextDouble();
    return getIndex(percentRanges, randomPercent);
  }
}
