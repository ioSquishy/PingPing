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
        InteractionFollowupMessageBuilder response = this.interaction.createFollowupMessageBuilder();
        try {
            long server_id = this.interaction.getArgumentLongValueByName(TwitchSub.Columns.SERVER_ID.dcmd_argument_name).orElseThrow();
            String streamer = this.interaction.getArgumentStringValueByName(TwitchSub.Columns.BROADCASTER_ID.dcmd_argument_name).orElseThrow();
            long role_id = this.interaction.getArgumentRoleValueByName(TwitchSub.Columns.PINGROLE_ID.dcmd_argument_name).orElseThrow().getId();
            long channel_id = this.interaction.getArgumentChannelValueByName(TwitchSub.Columns.PINGCHANNEL_ID.dcmd_argument_name).orElseThrow().getId();
            registerSub(server_id, streamer, role_id, channel_id);
            Logger.trace("RegisterTwitchSub command ran.");
        } catch (NoSuchElementException e) {
            Logger.error(e, "Discord command argument missing for command: {}", commandName);
            response.setContent("Command failed; Missing an argument.").send();
            return;
        } catch (InvalidArgumentException | TwitchApiException | DatabaseException e) {
            Logger.debug(e);
            response.setContent("Command failed; " + e.getMessage()).send();
        } catch (Exception e) {
            Logger.error(e, "Unforseen exception in command: {}", commandName);
            response.setContent("Command failed for unforseen reason...").send();
        }
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
        Database.TwitchSubsTable.insertSubscription(sub);
    }
}
