package pingping.Discord.Helpers;

import java.util.Optional;

import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.server.Server;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;
import java.awt.Color;

import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.User;
import com.google.api.services.youtube.model.Video;

import pingping.Database.OrmObjects.StreamerSubscription;
import pingping.Database.OrmObjects.TwitchSub;
import pingping.Database.OrmObjects.YoutubeSub;
import pingping.Discord.Constants;
import pingping.Discord.DiscordAPI;
import pingping.Exceptions.InvalidArgumentException;
import pingping.Exceptions.TwitchApiException;
import pingping.Twitch.TwitchAPI;

public class PushStreamNotification {
    private static final String twitch_stream_url_prefix = "https://www.twitch.tv/"; // + streamer_name
    private static final String youtube_stream_url_prefix = "https://www.youtube.com/live/"; // + video_id

    /**
     * Pushes a Twitch stream notification
     * @param sub subscription to push notification for
     * @param streamer_name streamer name (used for fallback URL if embed creation fails)
     */
    public static void pushTwitchStreamNotification(TwitchSub sub, String streamer_name) {
        pushStreamNotification(sub, streamer_name, null, null);
    }

    /**
     * Pushes a Youtube stream notification
     * @param sub subscription to push notification for
     * @param stream stream object with stream info for embed
     * @param pfp_url profile picture of streamer for embed
     */
    public static void pushYoutubeStreamNotification(YoutubeSub sub, Video stream, String pfp_url) {
        pushStreamNotification(sub, null, stream, pfp_url);
    }

    /**
     * Pushes a stream notification to server for that sub
     * @param sub subscription to push notification for
     * @param streamer_name (for Twitch) name of streamer
     * @param yt_stream (for Youtube) Video object of stream
     * @param yt_pfp_url (for Youtube) URL of streamer profile picture
     */
    private static void pushStreamNotification(StreamerSubscription sub, String streamer_name, Video yt_stream, String yt_pfp_url) {
        Logger.trace("Attempting to push stream notification for broadcaster id {} in server id {}", sub.broadcaster_id, sub.server_id);

        Optional<Server> optionalServer = DiscordAPI.getAPI().getServerById(sub.server_id);
        if (optionalServer.isEmpty()) {
            // remove server from database. assumes that bot was removed from server while offline
            Logger.warn("Subscription with server with id {} was not found. Removing from database.");
            RemoveServer.removeServer(sub.server_id);
            return;
        }

        Server server = optionalServer.get();
        server.getChannelById(sub.pingchannel_id).ifPresentOrElse(generalDiscordChannel -> {
            generalDiscordChannel.asServerTextChannel().ifPresentOrElse(discordServerTextChannel -> {
                server.getRoleById(sub.pingrole_id).ifPresentOrElse(discordPingRole -> {

                    // check if bot has permission to send messages
                    if (!discordServerTextChannel.hasPermissions(DiscordAPI.getAPI().getYourself(), PermissionType.SEND_MESSAGES, PermissionType.VIEW_CHANNEL)) {
                        Logger.debug("Bot does not have permission to send messages in channel with id: {}", discordServerTextChannel.getId());
                        server.getOwner().ifPresent(owner -> owner.sendMessage("I do not have permission to send messages in " + discordServerTextChannel.getMentionTag()).join());

                        return;
                    }

                    // differences between twitch/youtube sub here:
                    if (sub.getClass() == TwitchSub.class) {
                        // if twitch sub
                        String streamLink = twitch_stream_url_prefix + streamer_name;
                        try {
                            new MessageBuilder()
                                .setContent(discordPingRole.getMentionTag())
                                .addEmbed(createTwitchStreamOnlineEmbed(sub.broadcaster_id, discordPingRole.getColor()))
                                .addActionRow(Button.link(streamLink, "Watch Stream"))
                                .send(discordServerTextChannel);
                        } catch (Exception e) {
                            Logger.error(e, "Failed to create embed for Twitch stream notification. Falling back with simple message.");
                            discordServerTextChannel.sendMessage(discordPingRole.getMentionTag() + " [" + streamer_name + " went live on Twitch!" + "](" + streamLink + ")");
                        }
                    } else {
                        // if youtube sub
                        YoutubeSub ytSub = (YoutubeSub) sub;
                        String streamLink = youtube_stream_url_prefix + ytSub.last_stream_video_id;
                        try {
                            new MessageBuilder()
                                .setContent(discordPingRole.getMentionTag())
                                .addEmbed(createYoutubeStreamOnlineEmbed(yt_stream, yt_pfp_url, discordPingRole.getColor()))
                                .addActionRow(Button.link(streamLink, "Watch Stream"))
                                .send(discordServerTextChannel);
                        } catch (Exception e) {
                            Logger.error(e, "Failed to create embed for Youtube stream notification. Falling back with simple message.");
                            discordServerTextChannel.sendMessage(discordPingRole.getMentionTag() + " [" + ytSub.broadcaster_handle + " went live on Twitch!" + "](" + streamLink + ")");
                        }
                    }
                    
                }, () -> { Logger.debug("Discord role with id {} in {} does not exist", sub.pingrole_id, sub.server_id); });
            }, () -> { Logger.debug("Discord channel with id {} in server {} is not a ServerTextChannel", sub.pingchannel_id, sub.server_id);} );
        }, () -> { Logger.debug("Discord channel with id {} in server {} does not exist", sub.pingchannel_id, sub.server_id);} );

        Logger.debug("Pushed stream notification for broadcaster id {} in server id {}", sub.broadcaster_id, sub.server_id);
    }

    public static EmbedBuilder createTwitchStreamOnlineEmbed(@NotNull String broadcaster_id, Optional<Color> color) throws TwitchApiException, InvalidArgumentException {
        Stream twitchStream = TwitchAPI.getStream(broadcaster_id);
        User twitchStreamer = TwitchAPI.getUserById(broadcaster_id);
        String streamLink = twitch_stream_url_prefix + twitchStreamer.getLogin();
        return new EmbedBuilder()
            .setAuthor(twitchStreamer.getDisplayName() + " went live on Twitch!", streamLink, twitchStreamer.getProfileImageUrl())
            .setTitle(twitchStream.getTitle())
            .setUrl(streamLink)
            .addField("Game", twitchStream.getGameName())
            .setColor(color.orElse(Constants.twitch_purple))
            .setTimestampToNow();
    }

    public static EmbedBuilder createYoutubeStreamOnlineEmbed(Video stream, String pfp_url, Optional<Color> color) {
        String streamLink = youtube_stream_url_prefix + stream.getId();
        return new EmbedBuilder()
            .setAuthor(stream.getSnippet().getChannelTitle() + " went live on Youtube!", streamLink, pfp_url)
            .setTitle(stream.getSnippet().getTitle())
            .setUrl(streamLink)
            .setColor(color.orElse(Constants.youtube_red))
            .setTimestampToNow();
    }
}
