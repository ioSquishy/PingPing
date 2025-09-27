package pingping.Discord.Helpers;

import java.util.List;

import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Database.OrmObjects.TwitchSub;
import pingping.Exceptions.DatabaseException;
import pingping.Exceptions.TwitchApiException;
import pingping.Twitch.TwitchConduit;

public class RemoveServer {
    /**
     * Safely removes server from database and deregisters any event subscriptions unique to this server
     */
    public static void removeServer(long server_id) {
        try {
            // unregister subscription if no other server is subscribed
            List<Long> broadcasterIds = Database.TwitchSubsTable.pullSubscriptionBroadcasterIds();
            for (Long broadcasterId : broadcasterIds) {
                List<TwitchSub> broadcasterSubs = Database.TwitchSubsTable.pullTwitchSubsFromBroadcasterId(broadcasterId);
                if (broadcasterSubs.size() == 1) { // size will be 1 if this is the only server with a subscription to the broadcaster
                    TwitchConduit.getConduit().unregisterSubscription(broadcasterSubs.get(0).eventsub_id);
                }
            }
            
            Database.ServerTable.removeEntry(server_id);
        } catch (TwitchApiException | DatabaseException e) {
            Logger.error(e, "Failed to remove server from database with id {}", server_id);
        }
    }
}
