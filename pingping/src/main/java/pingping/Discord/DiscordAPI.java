package pingping.Discord;

import org.tinylog.Logger;

import pingping.Discord.Commands.DiscordCommandFactory;

public class DiscordAPI {
    public static long bot_id = 0L;

    public static boolean connect() {
        Logger.trace("Connecting to Discord API...");
        DiscordCommandFactory.forceLoadCommandClasses();
        Logger.info("Discord API Connected");

        return true;
    }
}
