package pingping.Discord.Helpers;

import java.util.Optional;

import javax.annotation.Nullable;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.tinylog.Logger;
import java.awt.Color;

import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.User;

import pingping.Database.Database;
import pingping.Database.OrmObjects.TwitchSub;
import pingping.Discord.DiscordAPI;
import pingping.Exceptions.DatabaseException;
import pingping.Exceptions.InvalidArgumentException;
import pingping.Exceptions.TwitchApiException;
import pingping.Twitch.TwitchAPI;

public class PushStreamNotification {
    private static final String twitch_stream_url_prefix = "https://www.twitch.tv/";

    public static void pushTwitchStreamNotification(TwitchSub twitchSub, String streamer_name) {
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
                    try {
                        EmbedBuilder notificationEmbed = createTwitchStreamOnlineEmbed(twitchSub.broadcaster_id, discordPingRole.getColor().orElse(null));
                        discordServerTextChannel.sendMessage(notificationEmbed);
                    } catch (Exception e) {
                        Logger.error(e, "Failed to create embed for Twitch stream notification. Falling back with simple message.");
                        String streamLink = twitch_stream_url_prefix + streamer_name;
                        discordServerTextChannel.sendMessage(discordPingRole.getMentionTag() + " [" + streamer_name + " went live on Twitch!" + "](" + streamLink + ")");
                    }
                }, () -> { Logger.debug("Discord role with id {} in {} does not exist", twitchSub.pingrole_id, twitchSub.server_id); });
            }, () -> { Logger.debug("Discord channel with id {} in server {} is not a ServerTextChannel", twitchSub.pingchannel_id, twitchSub.server_id);} );
        }, () -> { Logger.debug("Discord channel with id {} in server {} does not exist", twitchSub.pingchannel_id, twitchSub.server_id);} );

        Logger.debug("Pushed stream notification for broadcaster id {} in server id {}", twitchSub.broadcaster_id, twitchSub.server_id);
    }

    public static EmbedBuilder createTwitchStreamOnlineEmbed(long broadcaster_id, @Nullable Color color) throws TwitchApiException, InvalidArgumentException {
        if (color == null) {
            color = new Color(100, 65, 165);
        }
        Stream twitchStream = TwitchAPI.getStream(broadcaster_id);
        User twitchStreamer = TwitchAPI.getUserById(broadcaster_id);
        String streamLink = twitch_stream_url_prefix + twitchStreamer.getLogin();
        return new EmbedBuilder()
            .setAuthor(twitchStreamer.getDisplayName() + " went live on Twitch!", streamLink, twitchStreamer.getProfileImageUrl())
            .setTitle(twitchStream.getTitle())
            .setUrl(streamLink)
            .addField("Game", twitchStream.getGameName())
            .setColor(color)
            .setTimestampToNow();
    }
}
