package pingping.Twitch;

import java.util.List;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.helix.domain.UserList;

import io.github.cdimascio.dotenv.Dotenv;

public class TwitchAPI {
    private static final TwitchClient twitchClient = TwitchClientBuilder.builder()
        .withClientId(Dotenv.load().get("TWITCH_CLIENT_ID"))
        .withClientSecret(Dotenv.load().get("TWITCH_SECRET"))
        .withEnableHelix(true)
        .build();

    public static String getChannelId(String channelName) {
        UserList users = twitchClient.getHelix().getUsers(null, null, List.of(channelName)).execute();
        if (users.getUsers().isEmpty()) {
            System.err.println("Error: Could not resolve username to channel ID for channelName: " + channelName);
            return null;
        }

        return users.getUsers().get(0).getId();
    }

}

