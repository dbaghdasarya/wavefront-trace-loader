package com.wavefront.datastructures;

import java.util.Collections;
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
    Collections.shuffle(distributions);
    calculateDistributionsPortions(count);
  }

  /**
   * Calculate corresponding portions of Distributions make as counts.
   */
  protected void calculateDistributionsPortions(int count) {
    double alreadyGenerated = 0;
    double accumulate = 0;
    double sumOfPercentages = distributions.stream().
        mapToDouble(t -> t.percentage).sum();
    for (int i = 0; i < distributions.size(); i++) {
      Distribution d = distributions.get(i);
      d.portion = d.percentage * count / sumOfPercentages;
      int portion = (int) Math.round(d.portion);
      if (Math.abs(accumulate) >= 1) {
        if (accumulate >= 0) {
          accumulate = accumulate - (d.portion - (int) d.portion);
          d.portion = (int) d.portion;
        } else {
          accumulate = accumulate + (d.portion - (int) d.portion);
          d.portion = Math.ceil(d.portion);
        }
      } else if (i == distributions.size() - 1) {
        d.portion = count - alreadyGenerated;
      } else {
        accumulate += portion - d.portion;
        d.portion = portion;
      }
      alreadyGenerated += d.portion;
    }
  }

  /**
   *  Pick a random index from distributions taking into account the weights of corresponding
   *  items. The algorithm was inspired from https://stackoverflow.com/questions/1761626/weighted-random-numbers
   */
  protected int weightedRand(int sumOfPercentages){
    int index = 0;
    int rand = RANDOM.nextInt(sumOfPercentages);
    for(int i = 0; i < distributions.size(); i++){
      if(distributions.get(i).percentage > rand){
        index = i;
      }
      else {
        rand -= distributions.get(i).percentage;
      }
    }
    return index;
  }

  @Override
  public T getNextDistribution() {
    if (distributions.isEmpty()) {
      return null;
    }
    int sumOfPercentages = (int) distributions.stream().
        mapToDouble(t -> t.percentage).sum();
    int index = weightedRand(sumOfPercentages);
    // Removing of depleted distributions.
    while (distributions.size() > 1 && distributions.get(index).portion <= 0) {
      sumOfPercentages -= distributions.get(index).percentage;
      distributions.remove(index);
      index = weightedRand(sumOfPercentages);
    }
    distributions.get(index).portion--;

    if (distributions.get(index).portion < -0.1) {
      distributions.remove(index);
      return null;
    }
    return distributions.get(index);
  }
}