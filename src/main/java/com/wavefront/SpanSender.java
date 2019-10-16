package com.wavefront;

import com.wavefront.sdk.common.WavefrontSender;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SpanSender {
    private static final Logger logger = Logger.getLogger(SpanSender.class.getCanonicalName());
    private WavefrontSender spanSender;


    public SpanSender(){
        ApplicationConfig config = ApplicationConfig.getInstance();
        spanSender =
                (new com.wavefront.sdk.direct.ingestion.WavefrontDirectIngestionClient.
                        Builder( config.getServer(), config.getToken())).build();
    }

    public void startSending( SpanQueue spanQueue) throws IOException, InterruptedException {
        Span tempSpan;
        while( (tempSpan = spanQueue.pollFirst()) != null){
            spanSender.sendSpan(
                    tempSpan.getName(),
                    tempSpan.getStartMillis(),
                    tempSpan.getDuration(),
                    tempSpan.getSource(),
                    tempSpan.getTraceUUID(),
                    tempSpan.getSpanUUID(),
                    tempSpan.getParents(),
                    null,
                    tempSpan.getTags(),
                    null);

            System.out.println("timestamp - " + tempSpan.getStartMillis());
            TimeUnit.SECONDS.sleep(1);
        }
        logger.log(Level.INFO, "Generation complete!");
    }
}
