package pingping.Discord.Events;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import com.github.twitch4j.eventsub.events.StreamOnlineEvent;

import pingping.Database.Database;
import pingping.Database.OrmObjects.TwitchSub;
import pingping.Discord.Helpers.PushStreamNotification;
import pingping.Exceptions.DatabaseException;
import pingping.Exceptions.TwitchApiException;
import pingping.Twitch.TwitchConduit;

public class TwitchStreamEvent extends DiscordEvent {
    public static final String event_name = "TwitchStreamEvent";
    static {
        DiscordEventRegistrar.registerEvent(event_name, TwitchStreamEvent::new);
    }
    protected TwitchStreamEvent() {
        super(event_name);
    }
    @Override
    protected void registerEventListener() {
        subscribeToTwitchStreamOnlineEvents();
    }
    
    public static void subscribeToTwitchStreamOnlineEvents() {
        try {
            TwitchConduit.getConduit().subscribeToStreamOnlineEvents(TwitchStreamEvent::handleStreamOnlineEvent);
            Logger.trace("Registered TwitchStreamOnlineEvent Listener");
        } catch (TwitchApiException | DatabaseException e) {
            Logger.error(e, "Failed to subscribe to twitch stream online events.");
        }
    }

    public static void handleStreamOnlineEvent(StreamOnlineEvent event) {
        String broadcasterId = event.getBroadcasterUserId();
        try {
            handleStreamOnlineEvent(broadcasterId, event.getBroadcasterUserName());
        } catch (DatabaseException e) {
            Logger.error(e, "Failed to handle stream online event for broadcaster with id {}", broadcasterId);
        }
    }

    public static void handleStreamOnlineEvent(@NotNull String broadcaster_id, String streamer_name) throws DatabaseException {
        List<TwitchSub> subscriptions = Database.TwitchSubsTable.pullTwitchSubsFromBroadcasterId(broadcaster_id);
        subscriptions.forEach(sub -> {
            PushStreamNotification.pushTwitchStreamNotification(sub, streamer_name);
        });
    }
}
