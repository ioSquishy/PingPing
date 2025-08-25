package pingping.Twitch;

import java.util.List;
import java.util.Optional;

import org.tinylog.Logger;

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

    public static Optional<Long> getChannelId(String channelName) {
        UserList users = twitchClient.getHelix().getUsers(null, null, List.of(channelName)).execute();
        try {
            if (!users.getUsers().isEmpty()) {
                return Optional.of(Long.valueOf(users.getUsers().get(0).getId()));
            }
        } catch (NumberFormatException e) {
            Logger.debug(e);
        } catch (NullPointerException e) {
            Logger.error(e);
        }
        return Optional.empty();
    }

}

