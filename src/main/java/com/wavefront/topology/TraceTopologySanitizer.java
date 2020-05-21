package com.wavefront.topology;

import com.fasterxml.jackson.databind.util.StdConverter;
import com.wavefront.datastructures.Distribution;
import com.wavefront.datastructures.TagVariation;
import com.wavefront.datastructures.TraceType;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static com.wavefront.helpers.Defaults.DEFAULT_MANDATORY_TAGS;
import static com.wavefront.helpers.Defaults.SERVICE;

/**
 * Sanitizer class which converts incorrect input values to default ones.
 *
 * @author Davit Baghdasaryan (dbaghdasarya@vmware.com)
 */
public class TraceTopologySanitizer extends StdConverter<TraceTopology, TraceTopology> {
  private static final Logger LOGGER = Logger.getLogger(TraceTopologySanitizer.class.getCanonicalName());

  @Override
  public TraceTopology convert(TraceTopology value) {
    // Check that at least one traceType exists.
    if (value.traceTypes == null || value.traceTypes.isEmpty()) {
      LOGGER.severe("At least one traceType and traceTopology must exist!");
      return null;
    }

    if (!traceTypeSanitizer(value) ||
        !serviceConnectionSanitizer(value) ||
        !serviceSpanNumbersSanitizer(value)) {
      return null;
    }

    serviceTagsSanitizer(value);

    return value;
  }

  private boolean traceTypeSanitizer(final TraceTopology value) {
    if (value.traceTypes == null || value.traceTypes.isEmpty()) {
      return false;
    }

    double sumOfTraceTypePercentages = 0;
    for (TraceType tt : value.traceTypes) {
      // Primitives.
      if (tt.debugRate < 0 || tt.debugRate > 100 ||
          tt.errorRate < 0 || tt.errorRate > 100 ||
          tt.spansCount < 1 || tt.spansCount > 10000 ||
          tt.tracePercentage < 0 || tt.tracePercentage > 100) {
        return false;
      }

      // TraceDurations.
      if (tt.traceDurations == null || tt.traceDurations.isEmpty()) {
        LOGGER.severe("At least one traceDuration must exist!");
        return false;
      }
      for (Distribution td : tt.traceDurations) {
        if (td.startValue > td.endValue || td.percentage < 0 || td.percentage > 100) {
          LOGGER.severe("Wrong format for traceDuration: startValue must be less or equal than " +
              "endValue, and percentage must be in range [0..100].");
          return false;
        }
      }

      // ErrorConditions.
      if (tt.errorConditions != null) {
        if (tt.errorConditions.removeIf(condition -> condition.errorRate < 1 || condition.errorRate > 100)) {
          LOGGER.warning("Some of the errorConditions were removed " +
              "because the errorRate was not in the [1..100] range!");
        }
        if (tt.errorConditions.isEmpty()) {
          tt.errorConditions = null;
          LOGGER.warning("Empty errorConditions block was completely " +
              "removed and the common errorRate will be applied!");
        }
      }

      sumOfTraceTypePercentages += tt.tracePercentage;
      Distribution.normalizeCanonicalDistributions(tt.traceDurations);
    }

    // Normalize TraceTypes probabilities.
    if (Double.compare(sumOfTraceTypePercentages, Distribution.HUNDRED_PERCENT) != 0) {
      final double ratio = 1.0 * Distribution.HUNDRED_PERCENT / sumOfTraceTypePercentages;
      value.traceTypes.forEach(tt -> tt.tracePercentage = tt.tracePercentage * ratio);
    }
    return true;
  }

  private boolean serviceConnectionSanitizer(final TraceTopology value) {
    // Check that at least one traceType exists.
    if (value.serviceConnections == null ||
        value.serviceConnections.isEmpty()) {
      LOGGER.severe("At least one serviceConnection must exist!");
      return false;
    } else {
      // Generate serviceMap from the serviceConnections.
      value.rootLevelServices = new HashSet<>();
      value.serviceConnections.forEach(connection -> {
        connection.services.forEach(service ->
            value.serviceInfos.computeIfAbsent(service, s -> new ServiceInfo()).
                children.addAll(connection.children));
        connection.children.forEach(child ->
            value.serviceInfos.computeIfAbsent(child, c -> new ServiceInfo()).
                parents.addAll(connection.services));

        if (connection.root) {
          value.rootLevelServices.addAll(connection.services);
        }
      });
    }

    // At least one root level service must exist.
    return !value.rootLevelServices.isEmpty();
  }

  private void serviceTagsSanitizer(final TraceTopology value) {
    // Check that each service has set of tags.
    if (value.serviceTags != null) {
      value.serviceTags.forEach(st -> {
        boolean isMandatoryTags = st.mandatoryTags != null && !st.mandatoryTags.isEmpty();
        boolean isOptionalTags = st.optionalTags != null && !st.optionalTags.isEmpty()
            && st.optionalTagsPercentage > 0;
        if ((isMandatoryTags || isOptionalTags) && st.services != null) {
          // Check and fix optionalTagsPercentage value
          if (st.optionalTagsPercentage > 100) {
            LOGGER.warning("Meaningless value of the optionalTagsPercentage = " +
                st.optionalTagsPercentage + " was replaced with 100%");
            st.optionalTagsPercentage = 100;
          }

          if (isOptionalTags) {
            st.optionalTags.forEach(tv -> tv.percentage = st.optionalTagsPercentage);
          }
          Consumer<ServiceInfo> addTags =
              (serviceInfo) -> {
                if (isMandatoryTags) {
                  st.mandatoryTags.forEach(tag -> serviceInfo.tags.
                      computeIfAbsent(tag.tagName, name -> new TagVariation(tag)).
                      tagValues.addAll(tag.tagValues));
                }
                if (isOptionalTags) {
                  st.optionalTags.forEach(tag -> serviceInfo.tags.
                      computeIfAbsent(tag.tagName, name -> new TagVariation(tag)).
                      tagValues.addAll(tag.tagValues));
                }
              };
          if (st.services.contains("*")) {
            value.serviceInfos.values().forEach(addTags);
          } else {
            st.services.forEach(s -> {
              ServiceInfo tempServiceInfo = value.serviceInfos.get(s);
              if (tempServiceInfo != null) {
                addTags.accept(tempServiceInfo);
              } else {
                LOGGER.warning(s + " - Service is redundant!");
              }
            });
          }
        } else {
          LOGGER.warning("ServiceTag is incomplete");
        }
      });
    }
    value.serviceInfos.forEach((k, v) ->
        v.tags.put(SERVICE, new TagVariation(SERVICE, Set.of(k))));

    DEFAULT_MANDATORY_TAGS.forEach(tagVariation -> value.serviceInfos.forEach((k, v) -> {
          TagVariation tempTagVariation = v.tags.computeIfAbsent(
              tagVariation.tagName, name -> {
                LOGGER.warning(k + ": Missing mandatory tag was added - " + name);
                return new TagVariation(tagVariation);
              });
          if (tempTagVariation.tagValues.isEmpty()) {
            tempTagVariation.tagValues.addAll(tagVariation.tagValues);
            LOGGER.warning(k + ": Values for mandatory tag were added - " +
                tagVariation.tagName);
          }
        })
    );
  }

  private boolean serviceSpanNumbersSanitizer(final TraceTopology value) {
    if (value.serviceSpansNumbers != null &&
        !value.serviceSpansNumbers.isEmpty()) {
      for (TraceTopology.ServiceSpansNumber ssn : value.serviceSpansNumbers) {
        if (ssn.services == null || ssn.services.isEmpty() || ssn.spansNumber < 1) {
          return false;
        }

        if (ssn.services.contains("*")) {
          value.serviceInfos.values().forEach(si -> si.spansNumber = ssn.spansNumber);
        } else {
          ssn.services.forEach(s -> {
            ServiceInfo tempServiceInfo = value.serviceInfos.get(s);
            if (tempServiceInfo != null) {
              tempServiceInfo.spansNumber = ssn.spansNumber;
            } else {
              LOGGER.warning(s + " - Service is redundant!");
            }
          });
        }
      }
    }
    return true;
  }
}