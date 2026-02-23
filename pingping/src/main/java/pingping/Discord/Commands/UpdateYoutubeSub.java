package pingping.Discord.Commands;

import java.util.NoSuchElementException;
import java.util.Optional;

import javax.annotation.Nullable;

import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Database.OrmObjects.YoutubeChannel;
import pingping.Database.OrmObjects.YoutubeSub;
import pingping.Exceptions.DatabaseException;
import pingping.Exceptions.InvalidArgumentException;

public class UpdateYoutubeSub extends DiscordCommand {
    public static final String commandName = "updateyoutubesub";
    static {
        DiscordCommandFactory.registerCommand(commandName, UpdateYoutubeSub::new);
    }
    public UpdateYoutubeSub(SlashCommandInteraction interaction) {
        super(commandName, interaction);
    }
    @Override
    protected Optional<SlashCommandBuilder> getGlobalCommandBuilder() {
        return Optional.of(new SlashCommandBuilder()
            .setName(commandName)
            .setDescription("Update a Youtube subscription for this server.")
            .setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR)
            .setEnabledInDms(false)
            .addOption(new SlashCommandOptionBuilder()
                .setName(YoutubeSub.BROADCASTER_ID.DISCORD_CMD_ARG)
                .setDescription("Streamer to update notification settings for.")
                .setType(SlashCommandOptionType.STRING)
                .setRequired(true)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName(YoutubeSub.PINGROLE_ID.DISCORD_CMD_ARG)
                .setDescription("Role to ping when streamer goes live.")
                .setType(SlashCommandOptionType.ROLE)
                .setRequired(false)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName(YoutubeSub.PINGCHANNEL_ID.DISCORD_CMD_ARG)
                .setDescription("Discord channel to send stream notification in.")
                .setType(SlashCommandOptionType.CHANNEL)
                .setRequired(false)
                .build()));
    }
    @Override
    public void runCommand() {
        InteractionImmediateResponseBuilder response = this.interaction.createImmediateResponder();
        try {
            Logger.trace("{} discord command ran.", commandName);
            long server_id = this.interaction.getServer().get().getId();
            String streamer = this.interaction.getArgumentStringValueByName(YoutubeSub.BROADCASTER_ID.DISCORD_CMD_ARG).orElseThrow();

            Role role = this.interaction.getArgumentRoleValueByName(YoutubeSub.PINGROLE_ID.DISCORD_CMD_ARG).orElse(null);
            Long role_id = role != null ? role.getId() : null;

            ServerChannel channel = this.interaction.getArgumentChannelValueByName(YoutubeSub.PINGCHANNEL_ID.DISCORD_CMD_ARG).orElse(null);
            Long channel_id = channel != null ? channel.getId() : null;

            updateSub(server_id, streamer, role_id, channel_id);
            response.setContent("Subscription updated for: " + streamer).respond();
        } catch (NoSuchElementException e) {
            Logger.error(e, "Discord command argument missing for command: {}", commandName);
            response.setContent("Command failed; Missing an argument.").respond();
        } catch (InvalidArgumentException e) {
            Logger.debug(e);
            response.setContent(e.getMessage()).respond();
        } catch (DatabaseException e) {
            Logger.error(e);
            response.setContent(e.getMessage()).respond();
        } catch (Exception e) {
            Logger.error(e, "Unforeseen exception in command: {}", commandName);
            response.setContent("Command failed for unforeseen reason...").respond();
        }
    }

    public static void updateSub(long server_id, String streamer, @Nullable Long pingrole_id, @Nullable Long pingchannel_id) throws DatabaseException, InvalidArgumentException {
        Logger.trace("{} command ran with arguments: server_id={}, streamer={}, pingrole_id={}, pingchannel_id={}", commandName, server_id, streamer, pingrole_id, pingchannel_id);
        String broadcaster_id;
        YoutubeChannel yc = Database.YoutubeChannelsTable.getChannelFromHandle(streamer);
        if (yc == null) {
            throw new DatabaseException("Could not find existing subscription for specified Youtube channel.");
        } else {
            broadcaster_id = yc.broadcaster_id;
        }
        updateSub(server_id, broadcaster_id, Optional.ofNullable(pingrole_id), Optional.ofNullable(pingchannel_id));
        Logger.debug("Updated Youtube sub for streamer {} in server {}", streamer, server_id);
    }

    private static void updateSub(long server_id, @NotNull String broadcaster_id, Optional<Long> pingrole_id, Optional<Long> pingchannel_id) throws DatabaseException {
        // pull existing YoutubeSub and create a new one with existing or fields or new ones
        YoutubeSub existingSub = Database.YoutubeSubsTable.pullYoutubeSub(server_id, broadcaster_id);
        if (existingSub == null) {
            throw new IllegalArgumentException("Existing Youtube sub for this streamer not found.");
        }
        YoutubeSub updatedSub = new YoutubeSub(server_id, broadcaster_id, pingrole_id.orElse(existingSub.pingrole_id), pingchannel_id.orElse(existingSub.pingchannel_id), existingSub.uploads_playlist_id, existingSub.broadcaster_handle, existingSub.last_stream_video_id);

        // update in database
        Database.YoutubeSubsTable.updateSubscription(updatedSub);
    }
}
