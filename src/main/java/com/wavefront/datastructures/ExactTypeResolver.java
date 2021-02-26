package com.wavefront.datastructures;

import java.util.List;

import javax.annotation.Nonnull;

public class ExactTypeResolver extends TypeResolver {

  /**
   * @param itemsPortions List of items portions.
   * @return Return an index of the next item.
   */
  @Override
  public int getIndexOfNextItem(@Nonnull List<Double> itemsPortions) {
    for (int i = 0; i < itemsPortions.size(); i++) {
      if (itemsPortions.get(i) > 0) {
        itemsPortions.set(i, itemsPortions.get(i) - 1);
        return i;
      }
    }
    return RANDOM.nextInt(itemsPortions.size());
  }
}