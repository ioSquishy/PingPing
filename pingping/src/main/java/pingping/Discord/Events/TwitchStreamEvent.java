package pingping.Discord.Events;

import java.util.List;

import org.tinylog.Logger;

import com.github.twitch4j.eventsub.events.StreamOnlineEvent;

import pingping.Database.Database;
import pingping.Database.OrmObjects.TwitchSub;
import pingping.Discord.Helpers.PushStreamNotification;
import pingping.Exceptions.DatabaseException;

public class TwitchStreamEvent {
    public static void handleStreamOnlineEvent(StreamOnlineEvent event) {
        long broadcasterId = Long.parseLong(event.getBroadcasterUserId());
        try {
            handleStreamOnlineEvent(broadcasterId, event.getBroadcasterUserName());
        } catch (DatabaseException e) {
            Logger.error(e, "Failed to handle stream online event for broadcaster with id {}", broadcasterId);
        }
    }

    public static void handleStreamOnlineEvent(long broadcaster_id, String streamer_name) throws DatabaseException {
        List<TwitchSub> subscriptions = Database.TwitchSubsTable.pullTwitchSubsFromBroadcasterId(broadcaster_id);
        subscriptions.forEach(sub -> {
            PushStreamNotification.pushTwitchStreamNotification(sub, streamer_name);
        });
    }
}
