package pingping.Discord.Commands;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Database.OrmObjects.YoutubeChannel;
import pingping.Database.OrmObjects.YoutubeSub;
import pingping.Exceptions.DatabaseException;
import pingping.Exceptions.InvalidArgumentException;

public class UnregisterYoutubeSub extends DiscordCommand {
    public static final String commandName = "unregisteryoutubesub";
    static {
        DiscordCommandFactory.registerCommand(commandName, UnregisterYoutubeSub::new);
    }
    public UnregisterYoutubeSub(SlashCommandInteraction interaction) {
        super(commandName, interaction);
    }
    @Override
    protected Optional<SlashCommandBuilder> getGlobalCommandBuilder() {
        return Optional.of(new SlashCommandBuilder()
            .setName(commandName)
            .setDescription("Unregister a Youtube subscription for this server.")
            .setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR)
            .setEnabledInDms(false)
            .addOption(new SlashCommandOptionBuilder()
                .setName(YoutubeSub.BROADCASTER_ID.DISCORD_CMD_ARG)
                .setDescription("Streamer to unregister notification for.")
                .setType(SlashCommandOptionType.STRING)
                .setRequired(true)
                .build()));
    }
    @Override
    public void runCommand() {
        InteractionImmediateResponseBuilder response = this.interaction.createImmediateResponder();
        try {
            Logger.trace("{} discord command ran.", commandName);
            long server_id = this.interaction.getServer().get().getId();
            String streamer = this.interaction.getArgumentStringValueByName(YoutubeSub.BROADCASTER_ID.DISCORD_CMD_ARG).orElseThrow();
            unregisterSub(server_id, streamer);
            response.setContent("Subscription removed for: " + streamer).respond();
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

    /**
     * Unregister Youtube subscription for a server
     * If that server is the last subscription to the broadcaster, subscription will be removed from YoutubeChannelsTable as well
     * @param server_id
     * @param youtube_channel handle
     * @throws InvalidArgumentException if youtube channel doesn't exist
     * @throws DatabaseException if database modification fails
     */
    public static void unregisterSub(long server_id, String youtube_channel) throws InvalidArgumentException, DatabaseException {
        Logger.trace("{} command ran with arguments: server_id={}, youtube_channel={}", commandName, server_id, youtube_channel);
        // try to pull id from database first
        String broadcaster_id;
        YoutubeChannel yc = Database.YoutubeChannelsTable.getChannelFromHandle(youtube_channel);
        if (yc == null) {
            throw new InvalidArgumentException("Could not find existing subscription for specified Youtube channel.");
        } else {
            broadcaster_id = yc.broadcaster_id;
        }

        // get sub (if it exists)
        // this check is still necesarry because even if the sub exists in YoutubeChannelTable, this server SPECIFICALLY may not be subbed 
        YoutubeSub sub = Database.YoutubeSubsTable.pullYoutubeSub(server_id, broadcaster_id);
        if (sub == null) {
            throw new InvalidArgumentException("Could not find existing subscription for specified Youtube channel.");
        }

        // remove subscription from database
        Database.YoutubeSubsTable.removeSubscription(server_id, broadcaster_id);

        // if no more servers are subscribed, remove from YoutubeChannelsTable
        if (Database.YoutubeSubsTable.getNumSubsForBroadcasterId(broadcaster_id) == 0) {
            Database.YoutubeChannelsTable.removeChannel(broadcaster_id);
        }

        Logger.debug("Unregistered youtube sub for streamer {} from server {}", youtube_channel, server_id);
    }
}
