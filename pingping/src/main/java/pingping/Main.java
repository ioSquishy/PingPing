package pingping;

import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Discord.DiscordAPI;
import pingping.Discord.Events.ErrorLogEvent;
import pingping.Twitch.TwitchConduit;

public class Main {
    public static final byte INSTANCE_ID = 0;
    public static void main(String[] args) {
        // attempt accessing all major classes to initialize them, not necessary though
        try {
            Database.getConnection();
            TwitchConduit.getConduit();
            DiscordAPI.connect();
            ErrorLogEvent.setDmErrorsStatus(false);
        } catch (Exception e) {
            Logger.error(e, "Failed to start up successfully. Quitting.");
            System.exit(-1);
        }
    }
}