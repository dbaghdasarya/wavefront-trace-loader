package com.wavefront;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TraceTypePattern {
  public String traceTypeName;
  public int nestingLevel;
  public int spansNumber;
  public int tracesNumber;
//    public LinkedList<Pair<String, LinkedList<String> > > mandatoryTags;
//    public HashMap<String, LinkedList<String>> optionalTags;
}