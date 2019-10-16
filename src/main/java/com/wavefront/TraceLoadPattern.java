package com.wavefront;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Duration;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TraceLoadPattern {
    // spans per second
    private double rate;

    // load duration in minutes
    private Duration duration;

    public TraceLoadPattern(){}

    public Duration getDuration() {
        return duration;
    }

    public double getRate() {
        return rate;
    }
}
