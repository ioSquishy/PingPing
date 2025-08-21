package pingping;

import pingping.Twitch.TwitchAPI;
import pingping.Twitch.TwitchAuth;
import pingping.Twitch.TwitchConduit;

public class Main {
    // public static final DiscordApi api = new DiscordApiBuilder().setToken(Dotenv.load().get("DISCORD_TOKEN")).login().join();
    public static void main(String[] args) {
        Database.connect();
        Database.createTables();
        TwitchConduit conduit = TwitchConduit.getConduit(null);
        // System.out.println("registered: " + conduit.registerSubscription("asquishy"));
    }
}