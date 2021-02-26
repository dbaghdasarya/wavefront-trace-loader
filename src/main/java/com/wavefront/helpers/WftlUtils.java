package com.wavefront.helpers;

import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import javax.annotation.Nonnull;

import static com.wavefront.helpers.Defaults.HUNDRED_PERCENT;

/**
 * Collection of common helper methods.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public class WftlUtils {
  protected static final Random RANDOM = new Random(System.currentTimeMillis());

  /**
   * Randomly selects an item from the set.
   *
   * @param set Set of values
   * @param <T> Type of items in the set.
   * @return Randomly selected item.
   */
  public static <T> T getRandomFromSet(@Nonnull Set<T> set) {
    if (set.isEmpty()) {
      return null;
    }
    int rand = RANDOM.nextInt(set.size());
    Iterator<T> it = set.iterator();
    while (it.hasNext()) {
      if (rand == 0) {
        return it.next();
      }
      it.next();
      rand--;
    }
    return null;
  }

  /**
   * According to the percentage value randomly makes decision about probability.
   *
   * @param percentage Probability percentage.
   * @return True if the method decides that the case is possible.
   */
  public static boolean isEffectivePercentage(double percentage) {
    return (percentage > 0 && (RANDOM.nextDouble() <= (percentage / HUNDRED_PERCENT)));
  }
}
