package pingping;

import pingping.Twitch.TwitchAPI;
import pingping.Twitch.TwitchAuth;

public class Main {
    // public static final DiscordApi api = new DiscordApiBuilder().setToken(Dotenv.load().get("DISCORD_TOKEN")).login().join();
    public static void main(String[] args) {
        TwitchAPI.Conduit conduit = TwitchAPI.createConduit();
        System.out.println("registered: " + conduit.registerSubscription("asquishy"));
    }
}