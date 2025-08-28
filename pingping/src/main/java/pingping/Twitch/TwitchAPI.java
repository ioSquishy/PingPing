package pingping.Twitch;

import java.util.List;
import java.util.Optional;

import org.tinylog.Logger;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.eventsub.EventSubSubscription;
import com.github.twitch4j.helix.domain.EventSubSubscriptionList;
import com.github.twitch4j.helix.domain.UserList;
import com.netflix.hystrix.exception.HystrixBadRequestException;

import io.github.cdimascio.dotenv.Dotenv;

public class TwitchAPI {
    protected static final TwitchClient twitchClient = TwitchClientBuilder.builder()
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

    public static boolean deleteConduit(String conduit_id) {
        if (!twitchClient.getHelix().deleteConduit(TwitchAuth.appAccessToken, conduit_id).isSuccessfulExecution()) {
            TwitchAuth.refreshAppAccessToken();
        }
        return twitchClient.getHelix().deleteConduit(TwitchAuth.appAccessToken, conduit_id).isSuccessfulExecution();
    }

    public static List<EventSubSubscription> getEventSubSubs() {
        Logger.trace("getting event subs");
        EventSubSubscriptionList subs;
        try {
            subs = twitchClient.getHelix().getEventSubSubscriptions(TwitchAuth.appAccessToken, null, null, null, null, null).execute();
        } catch (HystrixBadRequestException e1) {
            Logger.warn(e1);
            try {
                TwitchAuth.refreshAppAccessToken();
                subs = twitchClient.getHelix().getEventSubSubscriptions(TwitchAuth.appAccessToken, null, null, null, null, null).execute();
            } catch (Exception e2) {
                Logger.error(e2);
                return null;
            }
        } catch (Exception ex) {
            Logger.error(ex);
            return null;
        }

        return subs.getSubscriptions();
    }
}

