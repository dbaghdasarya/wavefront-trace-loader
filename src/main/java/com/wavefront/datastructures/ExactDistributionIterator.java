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
    double alreadyGenerated = 0;
    Double accumulate = 0.0;
    double sumOfPercentages = distributions.stream().
        mapToDouble(t -> t.percentage).sum();
    for (int i = 0; i < distributions.size(); i++) {
      Distribution d = distributions.get(i);
      d.portion = d.percentage * count / sumOfPercentages;
      int portion = (int) Math.round(d.portion);
      if (accumulate >= 1) {
        accumulate = accumulate - (d.portion - (int) d.portion);
        d.portion = (int) d.portion;
        alreadyGenerated += d.portion;
      } else if (i == distributions.size() - 1) {
        d.portion = count - alreadyGenerated;
        alreadyGenerated += d.portion;
      } else {
        accumulate += portion - d.portion;
        d.portion = portion;
        alreadyGenerated += d.portion;
      }
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
    if (distributions.get(index).portion < -0.1) {
      distributions.remove(index);
      return null;
    }
    return distributions.get(index);
  }
}