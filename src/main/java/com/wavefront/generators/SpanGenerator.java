package com.wavefront.generators;

import com.wavefront.helpers.Statistics;

import java.util.Random;
import java.util.logging.Logger;

/**
 * Common interface for various span generators.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public abstract class SpanGenerator implements Runnable {
  protected static final Logger LOGGER = Logger.getLogger(ReIngestGenerator.class.getCanonicalName());
  protected static final Random RANDOM = new Random(System.currentTimeMillis());
  protected static final int HUNDRED_PERCENT = 100;

  /**
   * Generates spans for saving to file (without ingestion specific time delays).
   */
  public abstract void generateForFile();

  /**
   * Returns statistics about the generated traces.
   */
  public abstract Statistics getStatistics();
}