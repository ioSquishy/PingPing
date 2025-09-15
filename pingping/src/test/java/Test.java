import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Discord.DiscordAPI;
import pingping.Discord.Commands.*;
import pingping.Twitch.TwitchAPI;
import pingping.Twitch.TwitchConduit;

public class Test {
    public static void main(String[] args) {
        try {
            Database.getConnection();
            TwitchConduit.getConduit(0L);
        } catch (Exception e) {
            Logger.error(e, "Failed to start up successfully. Quitting.");
            return;
        }

        TwitchAPI.deleteAllExistingConduits();
    }
}
