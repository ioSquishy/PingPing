package pingping.Discord;

import pingping.Discord.Commands.DiscordCommand;
import pingping.Discord.Commands.DiscordCommandFactory;
import pingping.Discord.Commands.RegisterTwitchSub;

public class DiscordAPI {
    public static long bot_id = 0L;

    public static boolean connect() {
        DiscordCommandFactory.forceLoadCommandClasses();

        return true;
    }
}
