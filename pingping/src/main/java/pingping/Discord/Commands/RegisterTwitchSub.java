package pingping.Discord.Commands;

import java.util.Optional;

import org.javacord.api.interaction.SlashCommandInteraction;

import pingping.Discord.DiscordAPI;
import pingping.Discord.Exceptions.InvalidArgumentException;
import pingping.Twitch.TwitchAPI;
import pingping.Twitch.TwitchConduit;
import pingping.Twitch.TwitchSub;

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
        System.out.println("Ran from " + commandName + "!");
        // run run() with slash command arguments
    }

    public static void run(long server_id, String twitch_channel, long pingrole_id, long pingchannel_id) throws InvalidArgumentException {
        Optional<Long> broadcaster_id = TwitchAPI.getChannelId(twitch_channel);
        if (broadcaster_id.isPresent()) {
            run(server_id, broadcaster_id.get(), pingrole_id, pingchannel_id);
        } else {
            throw new InvalidArgumentException("Could not find twitch channel with name: " + twitch_channel);
        }
    }

    private static void run(long server_id, long broadcaster_id, long pingrole_id, long pingchannel_id) {
        Optional<String> subId = TwitchConduit.getConduit(DiscordAPI.bot_id).registerSubscription(broadcaster_id);
        if (subId.isEmpty()) {
            throw new UnknownError();
        }
    }

    private static void registerSub(TwitchSub sub) {

    }
}
