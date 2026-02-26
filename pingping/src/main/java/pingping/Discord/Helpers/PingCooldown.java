package pingping.Discord.Helpers;

import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PingCooldown {
    private static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final long COOLDOWN_TIME_MINS = 60L;
    private static HashSet<String> onCooldown = new HashSet<String>();

    private static Runnable removeCooldown(String broadcaster_id) {
        return () -> {
            onCooldown.remove(broadcaster_id);
        };
    }

    public static void putOnCooldown(String broadcaster_id) {
        onCooldown.add(broadcaster_id);
        scheduler.schedule(removeCooldown(broadcaster_id), COOLDOWN_TIME_MINS, TimeUnit.MINUTES);
    }

    public static boolean isOnCooldown(String broadcaster_id) {
        return onCooldown.contains(broadcaster_id);
    }
}
