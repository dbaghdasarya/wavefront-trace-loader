package com.wavefront;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TraceTypePattern {
  public String traceTypeName;
  public int nestingLevel;
  public int spansNumber;
  public int tracesNumber;
  public int errorRate;

  //    public LinkedList<Pair<String, LinkedList<String> > > mandatoryTags;
//    public HashMap<String, LinkedList<String>> optionalTags;

  public TraceTypePattern(String traceTypeName, int nestingLevel, int spansNumber,
                          int tracesNumber, int errorRate) {
    this.traceTypeName = traceTypeName;
    this.nestingLevel = nestingLevel;
    this.spansNumber = spansNumber;
    this.tracesNumber = tracesNumber;
    this.errorRate = errorRate;
  }

  public TraceTypePattern() {

  }
}