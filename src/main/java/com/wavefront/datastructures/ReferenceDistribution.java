package com.wavefront.datastructures;

/**
 * Templated class for storing links for any type of distributions.
 *
 * @author Norayr Chaparyan (nchaparyan@vmware.com)
 */
public class ReferenceDistribution<T> extends Distribution {
  public T reference;

  public ReferenceDistribution(T reference, double percentage) {
    this.reference = reference;
    this.percentage = percentage;
  }
}