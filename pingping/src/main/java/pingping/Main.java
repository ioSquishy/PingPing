package pingping;

import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Discord.DiscordAPI;
import pingping.Discord.Commands.RegisterTwitchSub;
import pingping.Discord.Commands.UnregisterTwitchSub;
import pingping.Twitch.TwitchConduit;

public class Main {
    // public static final DiscordApi api = new DiscordApiBuilder().setToken(Dotenv.load().get("DISCORD_TOKEN")).login().join();
    public static void main(String[] args) {
        try {
            Database.getConnection();
            DiscordAPI.connect();
            TwitchConduit.getConduit(DiscordAPI.bot_id);
        } catch (Exception e) {
            Logger.error(e, "Failed to start up successfully. Quitting.");
            return;
        }

        try {
            RegisterTwitchSub.registerSub(0L, "asquishy", 0L, 0L);
            // UnregisterTwitchSub.unregisterSub(0L, "asquishy");
        } catch (Exception e) {
            e.printStackTrace();
        }
        


        // get TwitchConduit
        // long bot_uid = 0L;
        // Database.GlobalTable.insertRow(bot_uid); // insert bot id if it doesn't exist
        // TwitchConduit conduit = TwitchConduit.getConduit(bot_uid); // create or get conduit

        // String sub_id = conduit.registerSubscription("asquishy").orElseThrow();
        // TwitchAPI.getEventSubSubs().forEach(sub -> {
        //     System.out.println("sub_id: " + sub.getId());
        // });

        // System.out.println("unsub success: " + conduit.unregisterSubscription(sub_id));
        // TwitchAPI.getEventSubSubs().forEach(sub -> {
        //     System.out.println("sub_id: " + sub.getId());
        // });
    }
}