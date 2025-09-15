package pingping;

import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Discord.DiscordAPI;
import pingping.Twitch.TwitchConduit;

public class Main {
    public static void main(String[] args) {
        try {
            Database.getConnection();
            DiscordAPI.connect();
            TwitchConduit.getConduit(DiscordAPI.getBotId());
        } catch (Exception e) {
            Logger.error(e, "Failed to start up successfully. Quitting.");
            return;
        }
    }
}