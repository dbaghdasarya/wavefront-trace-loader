package com.wavefront.datastructures;

import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;

/**
 * Base class to iterate via distributions.
 *
 * @author Norayr Chaparyan (nchaparyan@vmware.com)
 */
public abstract class DistributionIterator<T extends Distribution> {
  protected static final Random RANDOM = new Random(System.currentTimeMillis());
  protected List<T> distributions;

  protected DistributionIterator(@Nonnull List<T> distributions) {
    this.distributions = distributions;
  }

  /**
   * @return the next distribution to be generated.
   */
  public abstract T getNextDistribution();
}