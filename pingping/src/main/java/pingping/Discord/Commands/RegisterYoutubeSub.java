package pingping.Discord.Commands;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import com.google.api.services.youtube.model.Channel;

import pingping.Database.Database;
import pingping.Database.OrmObjects.YoutubeSub;
import pingping.Exceptions.DatabaseException;
import pingping.Exceptions.InvalidArgumentException;
import pingping.Exceptions.YoutubeApiException;
import pingping.Youtube.YoutubeAPI;

public class RegisterYoutubeSub extends DiscordCommand {
    public static final String commandName = "registeryoutubesub";
    static {
        DiscordCommandFactory.registerCommand(commandName, RegisterYoutubeSub::new);
    }
    public RegisterYoutubeSub(SlashCommandInteraction interaction) {
        super(commandName, interaction);
    }
    @Override
    protected Optional<SlashCommandBuilder> getGlobalCommandBuilder() {
        return Optional.of(new SlashCommandBuilder()
            .setName(commandName)
            .setDescription("Registered a youtube subscription for this server.")
            .setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR)
            .setEnabledInDms(false)
            .addOption(new SlashCommandOptionBuilder()
                .setName(YoutubeSub.BROADCASTER_HANDLE.DISCORD_CMD_ARG)
                .setDescription("Streamer to register notification for.")
                .setType(SlashCommandOptionType.STRING)
                .setRequired(true)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName(YoutubeSub.PINGROLE_ID.DISCORD_CMD_ARG)
                .setDescription("Role to ping when streamer goes live.")
                .setType(SlashCommandOptionType.ROLE)
                .setRequired(true)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName(YoutubeSub.PINGCHANNEL_ID.DISCORD_CMD_ARG)
                .setDescription("Discord channel to send stream notification in.")
                .setType(SlashCommandOptionType.CHANNEL)
                .setRequired(true)
                .build()));
    }
    @Override
    public void runCommand() {
        InteractionImmediateResponseBuilder response = this.interaction.createImmediateResponder();
        try {
            Logger.trace("{} discord command ran.", commandName);
            long server_id = this.interaction.getServer().get().getId();
            String streamer = this.interaction.getArgumentStringValueByName(YoutubeSub.BROADCASTER_HANDLE.DISCORD_CMD_ARG).orElseThrow();
            long role_id = this.interaction.getArgumentRoleValueByName(YoutubeSub.PINGROLE_ID.DISCORD_CMD_ARG).orElseThrow().getId();
            long channel_id = this.interaction.getArgumentChannelValueByName(YoutubeSub.PINGCHANNEL_ID.DISCORD_CMD_ARG).orElseThrow().getId();
            registerSub(server_id, streamer, role_id, channel_id);
            response.setContent("Subscription added for: " + streamer).respond();
        } catch (NoSuchElementException e) {
            Logger.error(e, "Discord command argument missing for command: {}", commandName);
            response.setContent("Command failed; Missing an argument.").respond();
        } catch (InvalidArgumentException e) {
            Logger.debug(e);
            response.setContent(e.getMessage()).respond();
        } catch (YoutubeApiException | DatabaseException e) {
            Logger.error(e);
            response.setContent(e.getMessage()).respond();
        } catch (Exception e) {
            Logger.error(e, "Unforeseen exception in command: {}", commandName);
            response.setContent("Command failed for unforeseen reason...").respond();
        }
    }

    // TODO: to make more quota-efficient, check if broadcaster uploads_playlist and broacaster_id is already in the database via youtube_handle and copy from there
    public static void registerSub(long server_id, @NotNull String youtube_handle, long pingrole_id, long pingchannel_id) throws InvalidArgumentException, YoutubeApiException, DatabaseException {
        Logger.trace("{} command ran with arguments: server_id={}, youtube_handle={}, pingrole_id={}", commandName, server_id, youtube_handle, pingrole_id, pingchannel_id);
        Channel youtubeChannel = YoutubeAPI.getChannel(youtube_handle);
        String broadcaster_id = YoutubeAPI.getChannelId(youtubeChannel);
        String uploads_playlist_id = YoutubeAPI.getChannelUploadsPlaylistId(youtubeChannel);
        registerSub(server_id, broadcaster_id, pingrole_id, pingchannel_id, uploads_playlist_id, youtube_handle);
        Logger.debug("Registered youtube sub for streamer {} in server {}", youtube_handle, server_id);
    }

    private static void registerSub(long server_id, @NotNull String broadcaster_id, long pingrole_id, long pingchannel_id, @NotNull String uploads_playlist_id, @NotNull String broadcaster_handle) throws DatabaseException, InvalidArgumentException {        
        // verify YoutubeSub with id does not already exist
        YoutubeSub potentialExistingSub = Database.YoutubeSubsTable.pullYoutubeSub(server_id, broadcaster_id);
        if (potentialExistingSub != null) {
            throw new InvalidArgumentException("Youtube sub for that streamer already exists. Use UpdateYoutubeSub instead.");
        }

        YoutubeSub sub = new YoutubeSub(server_id, broadcaster_id, pingrole_id, pingchannel_id, uploads_playlist_id, broadcaster_handle, null);

        // store YoutubeSub in database
        try {
            Database.YoutubeSubsTable.insertSubscription(sub);
        } catch (DatabaseException e) {
            Logger.error(e, "Successfully found and subscribed new Youtube sub, but failed to add entry to database.");
            throw e;
        }
    }
}
