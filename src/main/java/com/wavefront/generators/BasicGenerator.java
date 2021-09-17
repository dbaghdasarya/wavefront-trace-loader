package com.wavefront.generators;

import com.wavefront.DataQueue;
import com.wavefront.helpers.Statistics;

import java.util.Random;

import javax.annotation.Nonnull;

public abstract class BasicGenerator implements Runnable {
  protected static final Random RANDOM = new Random(System.currentTimeMillis());
  protected static final int SLEEP_DELAY_MILLISECONDS = 5;
  protected static final int GENERATION_DELAY_MILLIS = 5000;
  public final Statistics statistics = new Statistics();
  @Nonnull
  protected final DataQueue dataQueue;

  protected BasicGenerator(@Nonnull DataQueue dataQueue) {
    this.dataQueue = dataQueue;
  }

  /**
   * Generates traces for saving to file (without ingestion specific time delays).
   */
  public abstract void generateForFile();

  /**
   * Returns statistics about the generated traces.
   */
  public abstract Statistics getStatistics();
}
