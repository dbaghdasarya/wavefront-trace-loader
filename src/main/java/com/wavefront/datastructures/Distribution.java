package com.wavefront.datastructures;


/**
 * Base class for Distributions.
 *
 * @author Norayr Chaparyan (nchaparyan@vmware.com)
 */
public abstract class Distribution {
  /**
   * The portion of the current distribution over the distribution array.
   */
  public double portion = 0;
  /**
   * The percentage of the current distribution over the distribution array.
   */
  public double percentage;

  public Distribution() {
  }
}
