package com.wavefront.datastructures;

import java.util.List;

/**
 * An iterator for providing distributions to generate exact numbers of items according to
 * distributions count.
 *
 * @author Norayr Chaparyan (nchaparyan@vmware.com)
 */
public class ExactDistributionIterator<T extends Distribution> extends DistributionIterator<T> {

  public ExactDistributionIterator(List<T> distributions, int count) {
    super(distributions);
    calculateDistributionsPortions(count);
  }

  /**
   * Calculate corresponding portions of Distributions make as counts.
   */
  protected void calculateDistributionsPortions(int count) {
    double sumOfPercentages = distributions.stream().
        mapToDouble(t -> t.percentage).sum();

    for (Distribution d : distributions) {
      d.portion = d.percentage * count / sumOfPercentages;
    }
  }

  @Override
  public T getNextDistribution() {
    if (distributions.isEmpty()) {
      return null;
    }
    int index = RANDOM.nextInt(distributions.size());
    // Removing of depleted distributions.
    while (distributions.size() > 1 && distributions.get(index).portion <= 0) {
      distributions.remove(index);
      index = RANDOM.nextInt(distributions.size());
    }
    distributions.get(index).portion--;

    if (distributions.get(index).portion <= -1) {
      distributions.remove(index);
      return null;
    }
    return distributions.get(index);
  }
}