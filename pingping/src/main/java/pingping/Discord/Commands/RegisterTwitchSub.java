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

import pingping.Database.Database;
import pingping.Database.OrmObjects.TwitchSub;
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
                .setName(TwitchSub.BROADCASTER_ID.DISCORD_CMD_ARG)
                .setDescription("Streamer to register notification for.")
                .setType(SlashCommandOptionType.STRING)
                .setRequired(true)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName(TwitchSub.PINGROLE_ID.DISCORD_CMD_ARG)
                .setDescription("Role to ping when streamer goes live.")
                .setType(SlashCommandOptionType.ROLE)
                .setRequired(true)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName(TwitchSub.PINGCHANNEL_ID.DISCORD_CMD_ARG)
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
            String streamer = this.interaction.getArgumentStringValueByName(TwitchSub.BROADCASTER_ID.DISCORD_CMD_ARG).orElseThrow();
            long role_id = this.interaction.getArgumentRoleValueByName(TwitchSub.PINGROLE_ID.DISCORD_CMD_ARG).orElseThrow().getId();
            long channel_id = this.interaction.getArgumentChannelValueByName(TwitchSub.PINGCHANNEL_ID.DISCORD_CMD_ARG).orElseThrow().getId();
            registerSub(server_id, streamer, role_id, channel_id);
            response.setContent("Subscription added for: " + streamer).respond();
        } catch (NoSuchElementException e) {
            Logger.error(e, "Discord command argument missing for command: {}", commandName);
            response.setContent("Command failed; Missing an argument.").respond();
        } catch (InvalidArgumentException e) {
            Logger.debug(e);
            response.setContent(e.getMessage()).respond();
        } catch (TwitchApiException | DatabaseException e) {
            Logger.error(e);
            response.setContent(e.getMessage()).respond();
        } catch (Exception e) {
            Logger.error(e, "Unforeseen exception in command: {}", commandName);
            response.setContent("Command failed for unforeseen reason...").respond();
        }
    }

    public static void registerSub(long server_id, @NotNull String twitch_channel, long pingrole_id, long pingchannel_id) throws InvalidArgumentException, TwitchApiException, DatabaseException {
        Logger.trace("{} command ran with arguments: server_id={}, twitch_channel={}, pingrole_id={}", commandName, server_id, twitch_channel, pingrole_id, pingchannel_id);
        String broadcaster_id = TwitchAPI.getChannelId(twitch_channel);
        
        // verify TwitchSub with id does not already exist
        TwitchSub potentialExistingSub = Database.TwitchSubsTable.pullTwitchSub(server_id, broadcaster_id);
        if (potentialExistingSub != null) {
            throw new InvalidArgumentException("Twitch sub for that streamer already exists. Use UpdateTwitchSub instead.");
        }

        // register sub through twitch api and get event_sub id
        String subId = TwitchConduit.getConduit().registerSubscriptionById(broadcaster_id);

        // package sub details into a TwitchSub
        TwitchSub sub = new TwitchSub(server_id, broadcaster_id, pingrole_id, pingchannel_id, subId);
        
        // store TwitchSub in database
        try {
            Database.TwitchSubsTable.insertSubscription(sub);
        } catch (DatabaseException e) {
            Logger.error(e, "Successfully found and subscribed new twitch sub, but failed to add entry to database. Reverting changes...");
            // revert changes
            try {
                TwitchConduit.getConduit().unregisterSubscription(sub.eventsub_id);
                throw e;
            } catch (Exception e2) {
                Logger.error(e2, "Failed to revert changes.");
            }
        }

        Logger.debug("Registered twitch sub for streamer {} in server {}", twitch_channel, server_id);
    }
}
