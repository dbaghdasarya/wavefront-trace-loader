package com.wavefront;

import com.wavefront.sdk.common.Pair;
import com.wavefront.sdk.common.WavefrontSender;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class WavefrontTraceLoader {

    public static void main(String[] args) throws IOException, InterruptedException {

        if( args.length <= 0 || args[0] == null || args[0].isEmpty()){
            System.out.println("The first parameter is Pattern's file name, and can't be empty");
            return;
        }

        LoadExecutor loadExecutor = new LoadExecutor();
        loadExecutor.execute(args[0]);

//        WavefrontSender spanSender =
//                (new com.wavefront.sdk.direct.ingestion.WavefrontDirectIngestionClient.
//                        Builder( "http://localhost:8080", "bdc66030-a1a8-493b-b416-5559fdcfa45d")).build();
//
//        long time = Calendar.getInstance().getTimeInMillis() - 10;
//        List<Pair<String, String>> tags = new LinkedList<Pair<String, String>>();
//        tags.add(new Pair<String, String>("application", "trace loader"));
//        tags.add(new Pair<String, String>("service", "generator"));
//        tags.add(new Pair<String, String>("host", "ip-10.20.30.40"));
//        Random rand = new Random();
//        while(true){
//            time = Calendar.getInstance().getTimeInMillis() - 10;
//            spanSender.sendSpan("The First com.wavefront.traceloader.Span", time, 10, "localhost" + rand.nextInt(10),
//                    UUID.randomUUID(), UUID.randomUUID(), null, null, tags, null);
//            TimeUnit.SECONDS.sleep(1);
//            System.out.println("timestamp - " + time);
//        }
    }
}
