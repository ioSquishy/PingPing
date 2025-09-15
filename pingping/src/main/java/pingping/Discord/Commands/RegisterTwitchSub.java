package pingping.Discord.Commands;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionFollowupMessageBuilder;
import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Database.OrmObjects.TwitchSub;
import pingping.Discord.DiscordAPI;
import pingping.Exceptions.DatabaseException;
import pingping.Exceptions.InvalidArgumentException;
import pingping.Exceptions.TwitchApiException;
import pingping.Twitch.TwitchAPI;
import pingping.Twitch.TwitchConduit;

public class RegisterTwitchSub extends DiscordCommand {
    public static final String commandName = "registertwitchsub";
    static {
        DiscordCommandFactory.registerCommand(commandName, RegisterTwitchSub::new);
    }
    public RegisterTwitchSub(SlashCommandInteraction interaction) {
        super(commandName, interaction);
    }
    @Override
    protected Optional<SlashCommandBuilder> getGlobalCommandBuilder() {
        return Optional.of(new SlashCommandBuilder()
            .setName(commandName)
            .setDescription("Registered a twitch subscription for this server.")
            .setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR)
            .setEnabledInDms(false)
            .addOption(new SlashCommandOptionBuilder()
                .setName(TwitchSub.Columns.BROADCASTER_ID.dcmd_argument_name)
                .setDescription("Streamer to register notification for.")
                .setType(SlashCommandOptionType.STRING)
                .setRequired(true)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName(TwitchSub.Columns.PINGROLE_ID.dcmd_argument_name)
                .setDescription("Role to ping when streamer goes live.")
                .setType(SlashCommandOptionType.ROLE)
                .setRequired(true)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName(TwitchSub.Columns.PINGCHANNEL_ID.dcmd_argument_name)
                .setDescription("Discord channel to send stream notification in.")
                .setType(SlashCommandOptionType.CHANNEL)
                .setRequired(true)
                .build()));
    }
    @Override
    public void runCommand() {
        InteractionFollowupMessageBuilder response = this.interaction.createFollowupMessageBuilder();
        try {
            Logger.trace("RegisterTwitchSub discord command ran.");
            long server_id = this.interaction.getServer().get().getId();
            String streamer = this.interaction.getArgumentStringValueByName(TwitchSub.Columns.BROADCASTER_ID.dcmd_argument_name).orElseThrow();
            long role_id = this.interaction.getArgumentRoleValueByName(TwitchSub.Columns.PINGROLE_ID.dcmd_argument_name).orElseThrow().getId();
            long channel_id = this.interaction.getArgumentChannelValueByName(TwitchSub.Columns.PINGCHANNEL_ID.dcmd_argument_name).orElseThrow().getId();
            registerSub(server_id, streamer, role_id, channel_id);
            response.setContent("Subscription added.").send();
        } catch (NoSuchElementException e) {
            Logger.error(e, "Discord command argument missing for command: {}", commandName);
            response.setContent("Command failed; Missing an argument.").send();
        } catch (InvalidArgumentException e) {
            Logger.debug(e);
            response.setContent(e.getMessage()).send();
        } catch (TwitchApiException | DatabaseException e) {
            Logger.error(e);
            response.setContent(e.getMessage()).send();
        } catch (Exception e) {
            Logger.error(e, "Unforeseen exception in command: {}", commandName);
            response.setContent("Command failed for unforeseen reason...").send();
        }
    }

    public static void registerSub(long server_id, String twitch_channel, long pingrole_id, long pingchannel_id) throws InvalidArgumentException, TwitchApiException, DatabaseException {
        Logger.trace("Registering twitch sub for streamer {} in server {}", twitch_channel, server_id);
        Optional<Long> broadcaster_id = TwitchAPI.getChannelId(twitch_channel);
        if (broadcaster_id.isPresent()) {
            registerSub(server_id, broadcaster_id.get(), pingrole_id, pingchannel_id);
            Logger.debug("Registered twitch sub for streamer {} in server {}", twitch_channel, server_id);
        } else {
            throw new InvalidArgumentException("Could not find twitch streamer with name: " + twitch_channel);
        }
    }

    private static void registerSub(long server_id, long broadcaster_id, long pingrole_id, long pingchannel_id) throws TwitchApiException, DatabaseException, InvalidArgumentException {
        // verify TwitchSub with id does not already exist
        TwitchSub potentialExistingSub = Database.TwitchSubsTable.pullTwitchSub(server_id, broadcaster_id);
        if (potentialExistingSub != null) {
            throw new InvalidArgumentException("Twitch sub for that streamer already exists. Use UpdateTwitchSub instead.");
        }

        // register sub through twitch api and get event_sub id
        Optional<String> subId = TwitchConduit.getConduit(DiscordAPI.getBotId()).registerSubscription(broadcaster_id);
        if (subId.isEmpty()) {
            throw new TwitchApiException("Subscription registration through Twitch API was unsuccessful.");
        }

        // package sub details into a TwitchSub
        TwitchSub sub = new TwitchSub(server_id, broadcaster_id, subId.get(), pingrole_id, pingchannel_id);
        
        // store TwitchSub in database
        try {
            Database.TwitchSubsTable.insertSubscription(sub);
        } catch (DatabaseException e) {
            Logger.error(e, "Successfully found and subscripted new twitch sub, but failed to add entry to database. Reverting changes...");
            // revert changes
            try {
                TwitchConduit.getConduit(DiscordAPI.getBotId()).unregisterSubscription(sub.eventsub_id);
                throw e;
            } catch (Exception e2) {
                Logger.error(e2, "Failed to revert changes.");
            }
        }
    }
}
