package pingping.Twitch;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.eventsub.Conduit;
import com.github.twitch4j.eventsub.EventSubSubscription;
import com.github.twitch4j.helix.domain.EventSubSubscriptionList;
import com.github.twitch4j.helix.domain.UserList;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.exception.HystrixBadRequestException;

import io.github.cdimascio.dotenv.Dotenv;

public class TwitchAPI {
    protected static final TwitchClient twitchClient = TwitchClientBuilder.builder()
        .withClientId(Dotenv.load().get("TWITCH_CLIENT_ID"))
        .withClientSecret(Dotenv.load().get("TWITCH_SECRET"))
        .withEnableHelix(true)
        .build();

    public static Optional<Long> getChannelId(@NotNull String channelName) {
        UserList users = twitchClient.getHelix().getUsers(null, null, List.of(channelName)).execute();
        if (!users.getUsers().isEmpty()) {
            Long channelId = Long.valueOf(users.getUsers().get(0).getId());
            Logger.trace("getChannelId({}) -> {}", channelName, channelId);
            return Optional.of(channelId);
        }
        Logger.debug("Could not retrieve channel id of channel: {}", channelName);
        return Optional.empty();
    }

    public static void deleteAllExistingConduits() {
        List<Conduit> conduits = getConduitList();
        boolean allDeleted = true;

        for (Conduit conduit : conduits) {
                boolean success = deleteConduit(conduit.getId());
                if (success) {
                    Logger.debug("Deleted conduit with ID: {}", conduit.getId());
                } else {
                    allDeleted = false;
                    Logger.error("Failed to delete conduit with ID: {}", conduit.getId());
                }
        }
        
        if (allDeleted) {
            Logger.info("All Conduits Deleted");
        } else {
            Logger.error("Some conduits failed to delete.");
        }
    }

    public static List<Conduit> getConduitList() {
        try {
            return twitchClient.getHelix().getConduits(TwitchAuth.appAccessToken).execute().getConduits();
        } catch (Exception e1) {
            Logger.warn(e1);
            try {
                TwitchAuth.refreshAppAccessToken();
                return twitchClient.getHelix().getConduits(TwitchAuth.appAccessToken).execute().getConduits();
            } catch (Exception e2) {
                Logger.error(e2);
            }
        }
        return null;
    }

    public static boolean deleteConduit(String conduit_id) {
        return deleteConduit(conduit_id, true);
    }
    private static boolean deleteConduit(String conduit_id, boolean retry) {
        HystrixCommand<Void> command = twitchClient.getHelix().deleteConduit(TwitchAuth.appAccessToken, conduit_id);
        command.execute();
        if (command.isSuccessfulExecution()) {
            return true;
        } else {
            if (retry) {
                TwitchAuth.refreshAppAccessToken();
                return deleteConduit(conduit_id, false);
            } else {
                Logger.error(command.getExecutionException().getMessage());
                return false;
            }
        }
    }

    public static List<EventSubSubscription> getEventSubSubs() {
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

