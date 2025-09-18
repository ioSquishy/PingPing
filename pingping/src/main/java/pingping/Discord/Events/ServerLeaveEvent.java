package pingping.Discord.Events;

import org.javacord.api.listener.server.ServerLeaveListener;
import org.javacord.api.util.event.ListenerManager;
import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Discord.DiscordAPI;
import pingping.Exceptions.DatabaseException;

public class ServerLeaveEvent extends DiscordEvent {
    public static final String event_name = "ServerLeaveEvent";
    static {
        DiscordEventRegistrar.registerEvent(event_name, ServerLeaveEvent::new);
    }
    public ServerLeaveEvent() {
        super(event_name);
    }
    @Override
    public void registerEventListener() {
        registerServerLeaveListener();
    }
    
    private static ListenerManager<ServerLeaveListener> listener = null;
    public static ListenerManager<ServerLeaveListener> registerServerLeaveListener() {
        if (listener == null) {
            listener = DiscordAPI.getAPI().addServerLeaveListener(event -> {
                try {
                    Database.ServerTable.removeEntry(event.getServer().getId());
                } catch (DatabaseException e) {
                    Logger.error(e, "Failed to remove server from database with id {}", event.getServer().getId());
                }
            });
        }
        return listener;
    }
}
