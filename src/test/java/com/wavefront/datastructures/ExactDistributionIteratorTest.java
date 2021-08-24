package com.wavefront.datastructures;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class ExactDistributionIteratorTest {

  @Test
  public void testCalculateDistributionsPortionsWithValueDistribution() {
    int count = new Random().nextInt(100) + 1;
    System.out.println("TotalTraceCount : " + count);
    int countOfDistribution = new Random().nextInt(10) + 1;
    System.out.println("TraceTypeCount : " + countOfDistribution);
    List<ValueDistribution> distributions = new ArrayList<>();
    for (int i = 0; i < countOfDistribution; i++) {
      int percentage = new Random().nextInt(100);
      distributions.add(new ValueDistribution(1, 5, percentage));
    }
    ExactDistributionIterator<ValueDistribution> exactDistributionIterator =
        new ExactDistributionIterator<>(distributions, count);
    int sum = 0;
    for (ValueDistribution valueDistribution : distributions) {
      System.out.println("portion : " + valueDistribution.portion + ", percentage : " + valueDistribution.percentage);
      sum += valueDistribution.portion;
    }
    System.out.println("sumOfPortions : " + sum);
    assertEquals(sum, count);
  }
}
