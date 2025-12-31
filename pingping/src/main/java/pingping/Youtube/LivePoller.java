package pingping.Youtube;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import pingping.Database.Database;

public class LivePoller {
    private static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static Runnable pollApi() {
        return () -> {
            // pull youtube channels from database
            // use YoutubeApi.getActiveLivestream
            // if its active, make sure video id is not the same as last one stored in database
            // get all server ids with that broadcaster
            // store new last video id in database
            // use PushStreamNotification
        };
    }

    public static void startPolling() {
        if (scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }
        
        scheduler.scheduleAtFixedRate(pollApi(), 0, 1L, TimeUnit.MINUTES);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (scheduler != null) {
                try {
                    stopPolling();
                    System.out.println("LivePoller stopped by shutdown hook.");
                } catch (InterruptedException e) {
                    System.err.println("Error stopping LivePoller in shutdown hook.");
                    e.printStackTrace();
                }
            }
        }));
    }

    public static boolean stopPolling() throws InterruptedException {
        scheduler.shutdown();
        return scheduler.awaitTermination(5L, TimeUnit.SECONDS);
    }
}
