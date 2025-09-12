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
import org.javacord.api.interaction.callback.InteractionFollowupMessageBuilder;
import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Database.OrmObjects.TwitchSub;
import pingping.Exceptions.DatabaseException;
import pingping.Exceptions.InvalidArgumentException;
import pingping.Twitch.TwitchAPI;

public class UpdateTwitchSub extends DiscordCommand {
    public static final String commandName = "UpdateTwitchSub";
    static {
        DiscordCommandFactory.registerCommand(commandName, UpdateTwitchSub::new);
    }
    public UpdateTwitchSub(SlashCommandInteraction interaction) {
        super(commandName, interaction);
    }
    @Override
    protected Optional<SlashCommandBuilder> getGlobalCommandBuilder() {
        return Optional.of(new SlashCommandBuilder()
            .setName(commandName)
            .setDescription("Update a twitch subscription for this server.")
            .setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR)
            .setEnabledInDms(false)
            .addOption(new SlashCommandOptionBuilder()
                .setName(TwitchSub.Columns.BROADCASTER_ID.dcmd_argument_name)
                .setType(SlashCommandOptionType.STRING)
                .setRequired(true)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName(TwitchSub.Columns.PINGROLE_ID.dcmd_argument_name)
                .setDescription("Role to ping when streamer goes live.")
                .setType(SlashCommandOptionType.ROLE)
                .setRequired(false)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName(TwitchSub.Columns.PINGCHANNEL_ID.dcmd_argument_name)
                .setDescription("Discord channel to send stream notification in.")
                .setType(SlashCommandOptionType.CHANNEL)
                .setRequired(false)
                .build()));
    }
    @Override
    public void runCommand() {
        InteractionFollowupMessageBuilder response = this.interaction.createFollowupMessageBuilder();
        try {
            Logger.trace("UpdateTwitchSub discord command ran.");
            long server_id = this.interaction.getServer().get().getId();
            String streamer = this.interaction.getArgumentStringValueByName(TwitchSub.Columns.BROADCASTER_ID.dcmd_argument_name).orElseThrow();

            Role role = this.interaction.getArgumentRoleValueByName(TwitchSub.Columns.PINGROLE_ID.dcmd_argument_name).orElse(null);
            Long role_id = role != null ? role.getId() : null;

            ServerChannel channel = this.interaction.getArgumentChannelValueByName(TwitchSub.Columns.PINGCHANNEL_ID.dcmd_argument_name).orElse(null);
            Long channel_id = channel != null ? channel.getId() : null;

            updateSub(server_id, streamer, role_id, channel_id);
        } catch (NoSuchElementException e) {
            Logger.error(e, "Discord command argument missing for command: {}", commandName);
            response.setContent("Command failed; Missing an argument.").send();
            return;
        } catch (InvalidArgumentException e) {
            Logger.debug(e);
            response.setContent(e.getMessage()).send();
        } catch (DatabaseException e) {
            Logger.error(e);
            response.setContent(e.getMessage()).send();
        } catch (Exception e) {
            Logger.error(e, "Unforeseen exception in command: {}", commandName);
            response.setContent("Command failed for unforeseen reason...").send();
        }
    }

    public static void updateSub(long server_id, String streamer, @Nullable Long pingrole_id, @Nullable Long pingchannel_id) throws DatabaseException, InvalidArgumentException {
        Logger.trace("Updating twitch sub for channel {} in server {}", streamer, server_id);
        Optional<Long> broadcaster_id = TwitchAPI.getChannelId(streamer);
        if (broadcaster_id.isPresent()) {
            updateSub(server_id, broadcaster_id.get(), Optional.ofNullable(pingrole_id), Optional.ofNullable(pingchannel_id));
            Logger.debug("Updated twitch sub for streamer {} in server {}", streamer, server_id);
        } else {
            throw new InvalidArgumentException("Could not find streamer with name: " + streamer);
        }
    }

    private static void updateSub(long server_id, long broadcaster_id, Optional<Long> pingrole_id, Optional<Long> pingchannel_id) throws DatabaseException {
        // pull existing TwitchSub and create a new one with existing or fields or new ones
        TwitchSub existingSub = Database.TwitchSubsTable.pullTwitchSub(server_id, broadcaster_id);
        if (existingSub == null) {
            throw new IllegalArgumentException("Existing twitch sub for this streamer not found.");
        }
        TwitchSub updatedSub = new TwitchSub(server_id, broadcaster_id, existingSub.eventsub_id, pingrole_id.orElse(existingSub.pingrole_id), pingchannel_id.orElse(existingSub.pingchannel_id));

        // update in database
        Database.TwitchSubsTable.updateSubscription(updatedSub);
    }
}
