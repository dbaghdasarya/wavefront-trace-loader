package com.wavefront;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.io.IOException;

@ThreadSafe
public class LoadExecutor {

    // returns null if no errors
    synchronized public boolean execute(String patternFile) throws IOException, InterruptedException {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule());
        mapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
        TraceLoadPattern pattern;
        try {
            pattern = mapper.readValue(new File(patternFile), TraceLoadPattern.class);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error in the pattern file!");
            return false;
        }

        SpanGenerator spanGenerator = new SpanGenerator();
        SpanSender spanSender = new SpanSender();

        SpanQueue spanQueue = spanGenerator.generate(pattern);
        if(spanQueue != null){
            spanSender.startSending(spanQueue);
        }


        return true;
    }

}
