package com.wavefront.helpers;

import com.beust.jcommander.IStringConverter;

import java.time.Duration;

public class DurationStringConverter implements IStringConverter<Duration> {
    @Override
    public Duration convert(String value) {
        return Duration.parse("pt" + value);
    }
}