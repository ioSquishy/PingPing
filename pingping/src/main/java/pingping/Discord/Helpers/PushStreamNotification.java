package pingping.Discord.Helpers;

import java.util.Optional;

import org.javacord.api.entity.server.Server;
import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Database.OrmObjects.TwitchSub;
import pingping.Discord.DiscordAPI;
import pingping.Exceptions.DatabaseException;

public class PushStreamNotification {
    public static void pushTwitchStreamNotification(TwitchSub twitchSub) {
        Logger.trace("Pushing stream notification for broadcaster id {} in server id {}", twitchSub.broadcaster_id, twitchSub.server_id);

        Optional<Server> optionalServer = DiscordAPI.getAPI().getServerById(twitchSub.server_id);
        if (optionalServer.isEmpty()) {
            // remove server from database. assumes that bot was removed from server while offline
            Logger.warn("TwitchSub's server with id {} was not found. Removing from database.");
            try {
                Database.ServerTable.removeEntry(twitchSub.server_id);
            } catch (DatabaseException e) {
                Logger.error(e, "Failed to remove server with id {} from database.", twitchSub.server_id);
            }
            return;
        }

        Server server = optionalServer.get();
        server.getChannelById(twitchSub.pingchannel_id).ifPresentOrElse(generalDiscordChannel -> {
            generalDiscordChannel.asServerTextChannel().ifPresentOrElse(discordServerTextChannel -> {
                server.getRoleById(twitchSub.pingrole_id).ifPresentOrElse(discordPingRole -> {

                }, () -> { Logger.debug("Discord role with id {} in {} does not exist", twitchSub.pingrole_id, twitchSub.server_id); });
            }, () -> { Logger.debug("Discord channel with id {} in server {} is not a ServerTextChannel", twitchSub.pingchannel_id, twitchSub.server_id);} );
        }, () -> { Logger.debug("Discord channel with id {} in server {} does not exist", twitchSub.pingchannel_id, twitchSub.server_id);} );

        Logger.debug("Pushed stream notification for broadcaster id {} in server id {}", twitchSub.broadcaster_id, twitchSub.server_id);
    }

    public static void createStreamOnlineEmbed(String streamer, String stream_url, String pfp_url) {
        // TODO
    }
}
