package com.wavefront;

import com.wavefront.sdk.common.Pair;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SpanGenerator {
    private static final Logger logger = Logger.getLogger(SpanSender.class.getCanonicalName());

    public SpanQueue generate(TraceLoadPattern pattern){
        SpanQueue spanQueue = new SpanQueue();
        long spansNumber = (long)(pattern.getRate() * pattern.getDuration().toSeconds());
        long spanDuration = (long)(1000.0 / pattern.getRate());
        logger.log( Level.INFO, "Should be genereted " + spansNumber + " spans, with spanDuration - " +
                spanDuration);
        long startTime = Calendar.getInstance().getTimeInMillis();
        long currentTime = startTime;
        Random rand = new Random();
        String suffixes = "abcdefg";
        int sufLen = suffixes.length();

        List<Pair<String, String>> tags = new LinkedList<Pair<String, String>>();
        tags.add(new Pair<String, String>("application", "trace loader"));
        tags.add(new Pair<String, String>("service", "generator"));
        tags.add(new Pair<String, String>("host", "ip-10.20.30.40"));

        for(long n = 0; n < spansNumber; n++){
            spanQueue.addLast(new Span(
                    "name_" + suffixes.charAt(rand.nextInt(sufLen)),
                    currentTime,
                    spanDuration,
                    "localhost", // + suffixes.charAt(rand.nextInt(sufLen)),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    null,
                    null,
                    tags,
                    null));

            currentTime += spanDuration;
        }

        logger.log(Level.INFO, "Generation complete!");
        return spanQueue;
    }
}
