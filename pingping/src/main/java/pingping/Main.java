package pingping;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;

import com.github.twitch4j.eventsub.EventSubSubscription;

import io.github.cdimascio.dotenv.Dotenv;
import pingping.Twitch.TwitchAPI;
import pingping.Twitch.TwitchAuth;

public class Main {
    // public static final DiscordApi api = new DiscordApiBuilder().setToken(Dotenv.load().get("DISCORD_TOKEN")).login().join();
    public static void main(String[] args) {
        if (!TwitchAuth.InitTokenRequests()) {
            return;
        }
        TwitchAPI.InitClient(TwitchAuth.appAccessToken);
        EventSubSubscription sub = TwitchAPI.RegisterStreamOnlineNotif("asquishy");
        System.out.println("unsub success: " + TwitchAPI.UnregisterSubscription(sub));
    }
}