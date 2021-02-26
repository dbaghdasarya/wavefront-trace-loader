package com.wavefront.datastructures;

import java.util.List;

import javax.annotation.Nonnull;

public class RandomTypeResolver extends TypeResolver {
  /**
   * @param itemsPortions List of items portions.
   * @return Return an index of the next item.
   */
  @Override
  public int getIndexOfNextItem(@Nonnull List<Double> itemsPortions) {
    return getIndex(itemsPortions);
  }
}