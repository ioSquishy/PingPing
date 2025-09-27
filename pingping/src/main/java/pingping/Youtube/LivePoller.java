package pingping.Youtube;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LivePoller {
    
    private static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static Runnable pollApi() {
        return () -> {
            // TODO create a thread that polls channels in database every minute
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
