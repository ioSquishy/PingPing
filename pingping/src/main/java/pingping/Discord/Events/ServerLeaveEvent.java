package pingping.Discord.Events;

import java.util.List;

import org.javacord.api.listener.server.ServerLeaveListener;
import org.javacord.api.util.event.ListenerManager;
import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Database.OrmObjects.TwitchSub;
import pingping.Discord.DiscordAPI;
import pingping.Exceptions.DatabaseException;
import pingping.Exceptions.TwitchApiException;
import pingping.Twitch.TwitchConduit;

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
                try {
                    // unregister subscription if no other server is subscribed
                    List<Long> serverBroadcasterIdSubs = Database.TwitchSubsTable.pullSubscriptionBroadcasterIds();
                    for (Long broadcasterId : serverBroadcasterIdSubs) {
                        List<TwitchSub> subsToBroadcaster = Database.TwitchSubsTable.pullTwitchSubsFromBroadcasterId(broadcasterId);
                        if (subsToBroadcaster.size() == 1) { // size will be 1 if this is the only server with a subscription to the broadcaster
                            TwitchConduit.getConduit().unregisterSubscription(subsToBroadcaster.get(0).eventsub_id);
                        }
                    }
                    
                    Database.ServerTable.removeEntry(event.getServer().getId());
                } catch (TwitchApiException | DatabaseException e) {
                    Logger.error(e, "Failed to remove server from database with id {}", event.getServer().getId());
                }
            });
            Logger.trace("Registered ServerLeaveListener");
        }
        return listener;
    }
}
