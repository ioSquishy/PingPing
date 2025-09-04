package pingping.Discord;

import pingping.Discord.Commands.DiscordCommandFactory;

public class DiscordAPI {
    public static long bot_id = 0L;

    public static boolean connect() {
        DiscordCommandFactory.forceLoadCommandClasses();

        return true;
    }
}
