package com.wavefront.datastructures;

import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;

public abstract class TypeResolver {
  protected final Random RANDOM = new Random(System.currentTimeMillis());

  /**
   * @param itemsPortions List of items portions.
   * @return Return an index of the next item.
   */
  public abstract int getIndexOfNextItem(@Nonnull List<Double> itemsPortions);

  /**
   * Used binary search algorithm to find index.
   */
  public int getIndex(@Nonnull List<Double> itemsPortions) {
    final double randomNumber = itemsPortions.get(itemsPortions.size() - 1) * RANDOM.nextDouble();
    int firstIndex = 0;
    int lastIndex = itemsPortions.size() - 1;
    int middleIndex = 0;

    while (firstIndex <= lastIndex) {
      middleIndex = firstIndex + (lastIndex - firstIndex) / 2;
      if (Double.compare(itemsPortions.get(middleIndex), randomNumber) == 0) {
        return middleIndex;
      }
      if (Double.compare(itemsPortions.get(middleIndex), randomNumber) < 0) {
        firstIndex = middleIndex + 1;
      } else {
        lastIndex = middleIndex - 1;
      }
    }
    return middleIndex;
  }
}
