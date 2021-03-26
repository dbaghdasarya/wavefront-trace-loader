package com.wavefront.datastructures;

import java.util.List;

/**
 * An iterator for providing distributions to generate random numbers of items.
 *
 * @author Norayr Chaparyan (nchaparyan@vmware.com)
 */
public class RandomDistributionIterator<T extends Distribution> extends DistributionIterator<T> {
  public RandomDistributionIterator(List<T> distributions) {
    super(distributions);
    calculateDistributionsPortions();
  }

  /**
   * Calculate corresponding portions of Distributions make as ranges.
   */
  public void calculateDistributionsPortions() {
    double prevPortion = 0;
    for (Distribution d : distributions) {
      d.portion = d.percentage + prevPortion;
      prevPortion = d.portion;
    }
  }

  @Override
  public T getNextDistribution() {
    if (distributions.isEmpty()) {
      return null;
    }
    // A binary search algorithm is used to find the index of the next distribution.
    final double randomNumber = distributions.get(distributions.size() - 1).portion * RANDOM.nextDouble();
    int firstIndex = 0;
    int lastIndex = distributions.size() - 1;
    int middleIndex = 0;

    while (firstIndex <= lastIndex) {
      middleIndex = firstIndex + (lastIndex - firstIndex) / 2;
      if (Double.compare(distributions.get(middleIndex).portion, randomNumber) == 0) {
        return distributions.get(middleIndex);
      }
      if (distributions.get(middleIndex).portion < randomNumber) {
        firstIndex = middleIndex + 1;
      } else {
        lastIndex = middleIndex - 1;
      }
    }
    return distributions.get(middleIndex);
  }
}