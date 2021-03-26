package com.wavefront.datastructures;

import com.wavefront.sdk.common.Pair;

import java.util.List;

import static com.wavefront.helpers.Defaults.HUNDRED_PERCENT;

/**
 * Class represents condition on which error tag should be set for a span.
 *
 * @author Davit Baghdasaryan (dbagdasarya@vmware.com)
 */
public class ErrorCondition {
  /**
   * Span Names to which the error conditions should be applied. If null, will not be taken into
   * account.
   */
  public List<String> spanNames;
  /**
   * Name of a tag which is route cause of the error.
   */
  public String tagName;
  /**
   * Value of a tag which is a route cause of the error.
   */
  public String tagValue;
  /**
   * Errors rate for the given tag-value pair (percent).
   */
  public double errorRate;

  public static double getErrorRate(String spanName, List<Pair<String, String>> tags,
                                 List<ErrorCondition> errorConditions) {
    double errorRate = 0;
    for (ErrorCondition condition : errorConditions) {
      // If list of spanNames exists for this errorCondition,
      // check that the current span name is in the list
      if (condition.spanNames != null && !condition.spanNames.contains(spanName)) {
        continue;
      }

      if (tags.stream().
          anyMatch(tag -> tag._1.equals(condition.tagName) && tag._2.equals(condition.tagValue))) {
        // the effective Error Rate will be treated as a summary of probability of independent
        // events P(AB) = P(A) + P(B) - P(A) * P(B)
        errorRate += condition.errorRate - errorRate * condition.errorRate / HUNDRED_PERCENT;
        if (errorRate > HUNDRED_PERCENT) {
          errorRate = HUNDRED_PERCENT;
          break;
        }
      }
    }

    return errorRate;
  }
}
