package pingping.Discord.Commands;

import java.util.Optional;

import org.javacord.api.interaction.SlashCommandInteraction;
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
    public static final String commandName = "RegisterTwitchSub";
    static {
        DiscordCommandFactory.registerCommand(commandName, RegisterTwitchSub::new);
    }
    public RegisterTwitchSub(SlashCommandInteraction interaction) {
        super(commandName, interaction);
    }
    @Override
    public void runCommand() {
        Logger.trace("RegisterTwitchSub command ran.");
        // this.interaction.getArgumentLongValueByName()
        // run run() with slash command arguments
    }

    public static void registerSub(long server_id, String twitch_channel, long pingrole_id, long pingchannel_id) throws InvalidArgumentException, TwitchApiException, DatabaseException {
        Optional<Long> broadcaster_id = TwitchAPI.getChannelId(twitch_channel);
        if (broadcaster_id.isPresent()) {
            registerSub(server_id, broadcaster_id.get(), pingrole_id, pingchannel_id);
        } else {
            throw new InvalidArgumentException("Could not find twitch channel with name: " + twitch_channel);
        }
    }

    private static void registerSub(long server_id, long broadcaster_id, long pingrole_id, long pingchannel_id) throws TwitchApiException, DatabaseException {
        Optional<String> subId = TwitchConduit.getConduit(DiscordAPI.bot_id).registerSubscription(broadcaster_id);
        if (subId.isEmpty()) {
            throw new TwitchApiException("Subscription registration unsuccessful.");
        }

        TwitchSub sub = new TwitchSub(server_id, broadcaster_id, subId.get(), pingrole_id, pingchannel_id);
        boolean databaseStoreSuccess = Database.TwitchSubsTable.insertSubscription(sub);
        if (!databaseStoreSuccess) {
            throw new DatabaseException("Failed to store subscription in database.");
        }
    }
}
