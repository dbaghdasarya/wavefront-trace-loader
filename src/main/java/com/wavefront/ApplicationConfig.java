package com.wavefront;

public class ApplicationConfig {
    private static ApplicationConfig instance;
    private String server;
    private String token;

    private ApplicationConfig()
    {
        server = "http://localhost:8080";
        token = "bdc66030-a1a8-493b-b416-5559fdcfa45d";
    }

    public static ApplicationConfig getInstance()
    {
        if (instance == null)
        {
            //synchronized block to remove overhead
            synchronized (SpanQueue.class)
            {
                if(instance==null)
                {
                    // if instance is null, initialize
                    instance = new ApplicationConfig();
                }

            }
        }
        return instance;
    }

    public String getServer(){
        return server;
    }

    public String getToken(){
        return token;
    }

}
