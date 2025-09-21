package pingping.Discord.Events;

import java.util.concurrent.ExecutionException;

import org.tinylog.Level;
import org.tinylog.Logger;

import pingping.Discord.DiscordAPI;
import pingping.Logging.ApplicationLogEvent;
import pingping.Logging.ApplicationLogEventListener;
import pingping.Logging.ApplicationLogWriter;

public class ErrorLogEvent extends DiscordEvent {
    public static final String event_name = "ErrorLogEvent";
    static {
        DiscordEventRegistrar.registerEvent(event_name, ErrorLogEvent::new);
    }
    protected ErrorLogEvent() {
        super(event_name);
    }
    @Override
    protected void registerEventListener() {
        registerErrorLogEventListener();
    }

    private static boolean dm_errors_enabled = true;
    /**
     * set whether log level ERROR events should be direct messaged to bot owner
     * enabled by default
     * @param status if true, dms will be sent
     */
    public static void setDmErrorsStatus(boolean status) {
        dm_errors_enabled = status;
    }

    private static ApplicationLogEventListener listener = null;
    public static ApplicationLogEventListener registerErrorLogEventListener() {
        if (listener == null) {
            ApplicationLogWriter.registerLogEventListener(event -> {
                if (event.getLogEntry().getLevel() == Level.ERROR) {
                    if (dm_errors_enabled) {
                        sendErrorToBotOwner(event);
                    }
                }
            });
        }
        return listener;
    }

    private static void sendErrorToBotOwner(ApplicationLogEvent event) {
        if (DiscordAPI.getAPI() == null) {
            ApplicationLogWriter.deregisterLogEventListener(listener);
            listener = null;
            Logger.error("Discord API is null. Cannot send ERROR log to bot owner. Disabled ErrorLogEvent listener.");
        }
        DiscordAPI.getAPI().getOwner().ifPresentOrElse(botOwner -> {
            try {
                botOwner.get().sendMessage(event.getRenderedEntry());
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Failed to send error log to bot owner.");
            }
        }, () -> {
            System.err.println("Failed to send error log to bot owner.");
        });
    }
}
