package pingping.Discord.Events;

import org.javacord.api.listener.server.ServerLeaveListener;
import org.javacord.api.util.event.ListenerManager;
import org.tinylog.Logger;

import pingping.Discord.DiscordAPI;
import pingping.Discord.Helpers.RemoveServer;

public class ServerLeaveEvent extends DiscordEvent {
    public static final String event_name = "ServerLeaveEvent";
    static {
        DiscordEventRegistrar.registerEvent(event_name, ServerLeaveEvent::new);
    }
    protected ServerLeaveEvent() {
        super(event_name);
    }
    @Override
    protected void registerEventListener() {
        registerServerLeaveListener();
    }
    
    private static ListenerManager<ServerLeaveListener> listener = null;
    public static ListenerManager<ServerLeaveListener> registerServerLeaveListener() {
        if (listener == null) {
            listener = DiscordAPI.getAPI().addServerLeaveListener(event -> {
                RemoveServer.removeServer(event.getServer().getId());
            });
            Logger.trace("Registered ServerLeaveListener");
        }
        return listener;
    }
}
