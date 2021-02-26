package com.wavefront.datastructures;

import com.wavefront.helpers.WftlUtils;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

/**
 * Class represents a tag name and variation of its values.
 */
@SuppressWarnings("unused")
public class TagVariation {
  /**
   * Name of the tag.
   */
  public String tagName;
  /**
   * Possible values of the tag.
   */
  @Nonnull
  public final Set<String> tagValues;
  /**
   * Tag probability. 100 for mandatory tags and [1..100) for optional tags.
   */
  public double percentage = 100;

  public TagVariation(@Nonnull String tagName,
                      @Nonnull Set<String> tagValues) {
    this.tagName = tagName;
    this.tagValues = tagValues;
  }

  public TagVariation(@Nonnull String tagName,
                      @Nonnull Set<String> tagValues,
                      double percentage) {
    this.tagName = tagName;
    this.tagValues = tagValues;
    this.percentage = percentage;
  }

  public TagVariation() {
    this.tagValues = new HashSet<>();
  }

  public TagVariation(TagVariation source) {
    this.tagName = source.tagName;
    this.percentage = source.percentage;
    this.tagValues = new HashSet<>(source.tagValues);
  }

  public String getRandomValue() {
    return WftlUtils.getRandomFromSet(tagValues);
  }
}
