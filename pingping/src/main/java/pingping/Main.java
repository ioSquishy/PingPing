package pingping;

import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Discord.DiscordAPI;
import pingping.Discord.Commands.RegisterTwitchSub;
import pingping.Discord.Commands.UnregisterTwitchSub;
import pingping.Discord.Commands.UpdateTwitchSub;
import pingping.Twitch.TwitchConduit;

public class Main {
    // 
    public static void main(String[] args) {
        try {
            Database.getConnection();
            DiscordAPI.connect();
            TwitchConduit.getConduit(DiscordAPI.getBotId());
        } catch (Exception e) {
            Logger.error(e, "Failed to start up successfully. Quitting.");
            return;
        }

        try {
            // RegisterTwitchSub.registerSub(0L, "asquishy", 0L, 0L);
            // UnregisterTwitchSub.unregisterSub(0L, "asquishy");
            // UpdateTwitchSub.updateSub(1L, "asquishy", 0L, 0L);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}