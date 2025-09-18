package pingping.Twitch;

import java.util.List;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.eventsub.Conduit;
import com.github.twitch4j.eventsub.EventSubSubscription;
import com.github.twitch4j.eventsub.EventSubSubscriptionStatus;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.StreamList;
import com.github.twitch4j.helix.domain.User;
import com.github.twitch4j.helix.domain.UserList;
import com.netflix.hystrix.HystrixCommand;

import io.github.cdimascio.dotenv.Dotenv;
import pingping.Exceptions.InvalidArgumentException;
import pingping.Exceptions.TwitchApiException;

public class TwitchAPI {
    protected static final TwitchClient twitchClient = TwitchClientBuilder.builder()
        .withClientId(Dotenv.load().get("TWITCH_CLIENT_ID"))
        .withClientSecret(Dotenv.load().get("TWITCH_SECRET"))
        .withEnableHelix(true)
        .build();
    
    /**
     * @throws TwitchApiException if api request fails
     */
    private static List<User> getUsers(@Nullable List<String> userIds, @Nullable List<String> userNames) throws TwitchApiException {
        if (userIds == null && userNames == null) {
            Logger.warn("getStreams called with both userIds and userNames set to null");
        }
        try {
            UserList queryResult = twitchClient.getHelix().getUsers(null, userIds, userNames).execute();
            return queryResult.getUsers();
        } catch (Exception e) {
            Logger.error(e);
            throw new TwitchApiException("Failed to retrieve user(s) from Twitch API.");
        }
    }

    public static User getUserById(long user_id) throws TwitchApiException, InvalidArgumentException {
        List<User> users = getUsers(List.of(Long.toString(user_id)), null);
        if (!users.isEmpty()) {
            return users.get(0);
        } else {
            Logger.debug("Could not retrieve User of streamer with id: {}", user_id);
            throw new InvalidArgumentException("Could not find streamer with id: " + user_id);
        }
    }

    public static User getUserByName(@NotNull String userName) throws InvalidArgumentException, TwitchApiException {
        List<User> users = getUsers(null, List.of(userName));
        if (!users.isEmpty()) {
            return users.get(0);
        } else {
            Logger.debug("Could not retrieve User of streamer with name: {}", userName);
            throw new InvalidArgumentException("Could not find streamer with name: " + userName);
        }
    }

    /**
     * @throws TwitchApiException if api request fails
     */
    private static List<Stream> getStreams(@Nullable List<String> user_ids, @Nullable List<String> user_names, @Nullable Integer query_limit) throws TwitchApiException {
        if (user_ids == null && user_names == null) {
            Logger.warn("getStreams called with both user_ids and user_names set to null");
        }
        try {
            StreamList queryResult = twitchClient.getHelix().getStreams(null, null, null, query_limit, null, null, user_ids, user_names).execute();
            return queryResult.getStreams();
        } catch (Exception e) {
            Logger.error(e);
            throw new TwitchApiException("Failed to retrieve stream(s) from Twitch API.");
        }
    }

    public static Stream getStream(long user_id) throws TwitchApiException, InvalidArgumentException {
        List<Stream> stream = getStreams(List.of(Long.toString(user_id)), null, 1);
        if (!stream.isEmpty()) {
            return stream.get(0);
        } else {
            Logger.debug("Could not retrieve Stream of streamer with broadcaster id: {}", user_id);
            throw new InvalidArgumentException("Could not find streamer with id: " + user_id);
        }
    }

    public static long getChannelId(@NotNull String channelName) throws TwitchApiException, InvalidArgumentException {
        User user = getUserByName(channelName);
        long channelId = Long.parseLong(user.getId());
        Logger.trace("getChannelId({}) -> {}", channelName, channelId);
        return channelId;
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

    /**
     * Get enabled event subscriptions
     * @param userId if not null, will pull enabled subscriptions for this user only
     */
    public static List<EventSubSubscription> getEnabledEventSubscriptions(@Nullable String userId) {
        List<EventSubSubscription> subs;
        try {
            if (userId == null) {
                subs = twitchClient.getHelix().getEventSubSubscriptions(null, EventSubSubscriptionStatus.ENABLED, null, null, null, null).execute().getSubscriptions();
            } else {
                subs = twitchClient.getHelix().getEventSubSubscriptions(null, null, null, userId, null, null).execute().getSubscriptions()
                    .stream().filter(sub -> sub.getStatus().equals(EventSubSubscriptionStatus.ENABLED)).toList();
            }
        } catch (Exception e) {
            Logger.error(e);
            return null;
        }

        return subs;
    }
}

