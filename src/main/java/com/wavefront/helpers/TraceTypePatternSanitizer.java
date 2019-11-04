package com.wavefront.helpers;

import com.fasterxml.jackson.databind.util.StdConverter;
import com.wavefront.SpanSender;
import com.wavefront.TraceTypePattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class TraceTypePatternSanitizer extends StdConverter<TraceTypePattern, TraceTypePattern> {
  private static final Logger LOGGER = Logger.getLogger(SpanSender.class.getCanonicalName());
  private final static List<TraceTypePattern.TagVariation> DEFAULT_MANDATORY_TAGS =
      new ArrayList<>(
          List.of(
              new TraceTypePattern.TagVariation("application",
                  new ArrayList<>(Arrays.asList("Application_1", "Application_2"))),
              new TraceTypePattern.TagVariation("service",
                  new ArrayList<>(Arrays.asList("Service_1", "Service_2")))
          )
      );


  @Override
  public TraceTypePattern convert(TraceTypePattern value) {

    // Check and fix the tags set
    // Mandatory Tags must exist in the set and must have values
    if (value.mandatoryTags == null) {
      value.mandatoryTags = new ArrayList<>();
    }

    DEFAULT_MANDATORY_TAGS.forEach(tagVariation -> {
          TraceTypePattern.TagVariation tempTagVariation =
              value.mandatoryTags.stream().
                  filter(givenTag -> givenTag.tagName.equals(tagVariation.tagName)).
                  findFirst().
                  orElseGet(() -> {
                    value.mandatoryTags.add(tagVariation);
                    LOGGER.warning("Missing mandatory tag was added - " + tagVariation.tagName);
                    return tagVariation;
                  });

          if (tempTagVariation.tagValues.size() == 0) {
            tempTagVariation.tagValues.addAll(tagVariation.tagValues);
            LOGGER.warning("Values for mandatory tag were added - " + tagVariation.tagName);
          }
        }
    );

    // Check and fix optionalTagsPercentage value
    if (value.optionalTagsPercentage < 0 || value.optionalTagsPercentage > 100) {
      value.optionalTagsPercentage = 100;
      LOGGER.warning("Meaningless value of the optionalTagsPercentage was replaced with 100%");
    }
    return value;
  }
}
