package pingping.Twitch;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.eventsub.Conduit;
import com.github.twitch4j.eventsub.EventSubSubscription;
import com.github.twitch4j.helix.domain.EventSubSubscriptionList;
import com.github.twitch4j.helix.domain.User;
import com.github.twitch4j.helix.domain.UserList;
import com.netflix.hystrix.HystrixCommand;

import io.github.cdimascio.dotenv.Dotenv;
import pingping.Exceptions.TwitchApiException;

public class TwitchAPI {
    protected static final TwitchClient twitchClient = TwitchClientBuilder.builder()
        .withClientId(Dotenv.load().get("TWITCH_CLIENT_ID"))
        .withClientSecret(Dotenv.load().get("TWITCH_SECRET"))
        .withEnableHelix(true)
        .build();
    
    /**
     * 
     * @throws TwitchApiException if api request fails
     */
    public static List<User> getUsers(@Nullable List<String> userIds, @Nullable List<String> userNames) throws TwitchApiException {
        try {
            UserList queryResult = twitchClient.getHelix().getUsers(null, userIds, userNames).execute();
            return queryResult.getUsers();
        } catch (Exception e) {
            Logger.error(e);
            throw new TwitchApiException("Failed to retrieve user(s) from Twitch API.");
        }
    }

    public static Optional<Long> getChannelId(@NotNull String channelName) throws TwitchApiException {
        List<User> users = getUsers(null, List.of(channelName));
        if (!users.isEmpty()) {
            Long channelId = Long.valueOf(users.get(0).getId());
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
            return twitchClient.getHelix().getConduits(null).execute().getConduits();
        } catch (Exception e) {
            Logger.warn(e);
        }
        return null;
    }

    public static boolean deleteConduit(String conduit_id) {
        HystrixCommand<Void> command = twitchClient.getHelix().deleteConduit(null, conduit_id);
        command.execute();
        if (command.isSuccessfulExecution()) {
            return true;
        } else {
            Logger.error(command.getExecutionException().getMessage());
            return false;
        }
    }

    public static List<EventSubSubscription> getEventSubscriptions() {
        EventSubSubscriptionList subs;
        try {
            subs = twitchClient.getHelix().getEventSubSubscriptions(null, null, null, null, null, null).execute();
        } catch (Exception e) {
            Logger.error(e);
            return null;
        }

        return subs.getSubscriptions();
    }
}

