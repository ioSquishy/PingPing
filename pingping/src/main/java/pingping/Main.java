package pingping;

import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Discord.DiscordAPI;
import pingping.Twitch.TwitchConduit;

public class Main {
    public static final byte INSTANCE_ID = 0;
    public static void main(String[] args) {
        try {
            Database.getConnection();
            TwitchConduit.getConduit();
            DiscordAPI.connect();
        } catch (Exception e) {
            Logger.error(e, "Failed to start up successfully. Quitting.");
            System.exit(-1);
        }
    }
}