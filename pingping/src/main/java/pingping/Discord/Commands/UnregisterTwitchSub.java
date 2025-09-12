package pingping.Discord.Commands;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.javacord.api.interaction.SlashCommandInteraction;
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

public class UnregisterTwitchSub extends DiscordCommand {
    public static final String commandName = "UnregisterTwitchSub";
    static {
        DiscordCommandFactory.registerCommand(commandName, UnregisterTwitchSub::new);
    }
    public UnregisterTwitchSub(SlashCommandInteraction interaction) {
        super(commandName, interaction);
    }
    @Override
    public void runCommand() {
        InteractionFollowupMessageBuilder response = this.interaction.createFollowupMessageBuilder();
        try {
            Logger.trace("RegisterTwitchSub command ran.");
            long server_id = this.interaction.getServer().get().getId();
            String streamer = this.interaction.getArgumentStringValueByName(TwitchSub.Columns.BROADCASTER_ID.dcmd_argument_name).orElseThrow();
            unregisterSub(server_id, streamer);
        } catch (NoSuchElementException e) {
            Logger.error(e, "Discord command argument missing for command: {}", commandName);
            response.setContent("Command failed; Missing an argument.").send();
            return;
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

    public static void unregisterSub(long server_id, String twitch_channel) throws InvalidArgumentException, DatabaseException, TwitchApiException {
        Logger.trace("Unregistering twitch sub for streamer {} from server {}", twitch_channel, server_id);
        Optional<Long> broadcaster_id = TwitchAPI.getChannelId(twitch_channel);
        if (broadcaster_id.isPresent()) {
            unregisterSub(server_id, broadcaster_id.get());
            Logger.debug("Unregistered twitch sub for streamer {} from server {}", twitch_channel, server_id);
        } else {
            throw new InvalidArgumentException("Could not find twitch streamer with name: " + twitch_channel);
        }
    }

    private static void unregisterSub(long server_id, long broadcaster_id) throws DatabaseException, TwitchApiException {
        TwitchSub sub = Database.TwitchSubsTable.pullTwitchSub(server_id, broadcaster_id);
        if (sub == null) {
            throw new DatabaseException("Could not find existing subscription for specified twitch channel.");
        }

        boolean twitchApiUnsubSuccess = TwitchConduit.getConduit(DiscordAPI.bot_id).unregisterSubscription(sub.eventsub_id);
        if (twitchApiUnsubSuccess == false) {
            throw new TwitchApiException("Failed to unregister subscription with Twitch API.");
        }

        try {
            Database.TwitchSubsTable.removeSubscription(server_id, broadcaster_id);
        } catch (DatabaseException e) {
            Logger.error(e, "Successfully found and removed twitch subscription but failed to remove entry from database. Reverting changes...");
            // revert changes
            try {
                TwitchConduit.getConduit(DiscordAPI.bot_id).registerSubscription(broadcaster_id);
                throw e;
            } catch (Exception e2) {
                Logger.error(e2, "Failed to revert changes.");
            }
        }
    }
}
