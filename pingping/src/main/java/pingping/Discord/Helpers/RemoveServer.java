package pingping.Discord.Helpers;

import java.util.List;

import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Database.OrmObjects.TwitchSub;
import pingping.Database.OrmObjects.YoutubeSub;
import pingping.Exceptions.DatabaseException;
import pingping.Exceptions.TwitchApiException;
import pingping.Twitch.TwitchConduit;

public class RemoveServer {
    /**
     * Safely removes server from database and deregisters any event subscriptions unique to this server
     */
    public static void removeServer(long server_id) {
        try {
            // unregister twitch subscription if no other server is subscribed
            List<TwitchSub> twitchSubs = Database.TwitchSubsTable.pullTwitchSubsFromServerId(server_id);
            for (TwitchSub sub : twitchSubs) {
                if (Database.TwitchSubsTable.getNumSubsForBroadcasterId(sub.broadcaster_id) == 1) {
                    TwitchConduit.getConduit().unregisterSubscription(sub.eventsub_id);
                }
            }

            // remove youtube channel from channels table if this server has the only sub
            List<YoutubeSub> youtubeSubs = Database.YoutubeSubsTable.pullYoutubeSubsFromServerId(server_id);
            for (YoutubeSub sub : youtubeSubs) {
                if (Database.YoutubeSubsTable.getNumSubsForBroadcasterId(sub.broadcaster_id) == 1) {
                    Database.YoutubeChannelsTable.removeChannel(sub.broadcaster_id);
                }
            }
            
            // remove server from db, will clear subscription-level entries
            Database.ServerTable.removeEntry(server_id);
        } catch (TwitchApiException | DatabaseException e) {
            Logger.error(e, "Failed to remove server from database with id {}", server_id);
        }
    }
}
