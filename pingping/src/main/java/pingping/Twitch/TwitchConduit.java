package pingping.Twitch;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import com.github.philippheuer.events4j.api.IEventManager;
import com.github.twitch4j.eventsub.EventSubSubscription;
import com.github.twitch4j.eventsub.events.StreamOnlineEvent;
import com.github.twitch4j.eventsub.socket.IEventSubConduit;
import com.github.twitch4j.eventsub.socket.conduit.TwitchConduitSocketPool;
import com.github.twitch4j.eventsub.socket.conduit.exceptions.ConduitNotFoundException;
import com.github.twitch4j.eventsub.socket.conduit.exceptions.ConduitResizeException;
import com.github.twitch4j.eventsub.socket.conduit.exceptions.CreateConduitException;
import com.github.twitch4j.eventsub.socket.conduit.exceptions.ShardRegistrationException;
import com.github.twitch4j.eventsub.socket.conduit.exceptions.ShardTimeoutException;
import com.github.twitch4j.eventsub.socket.events.EventSocketSubscriptionFailureEvent;
import com.github.twitch4j.eventsub.socket.events.EventSocketSubscriptionSuccessEvent;
import com.github.twitch4j.eventsub.subscriptions.SubscriptionTypes;

import io.github.cdimascio.dotenv.Dotenv;
import pingping.Database;

public class TwitchConduit {
    private static TwitchConduit self = null;
    private static IEventSubConduit conduit;

    /**
     * Creates or retrieves a conduit
     * @param existing_conduit_id if provided, will search for existing conduit or return a new one
     */
    private TwitchConduit(@Nullable String existing_conduit_id) {
        if (setConduit(existing_conduit_id)) {
            self = this;
            registerEventListeners();
        }
    }

    /**
     * return true if successful
     * @param existing_conduit_id
     * @return
     */
    private static boolean setConduit(@Nullable String existing_conduit_id) {
        try {
            conduit = TwitchConduitSocketPool.create(spec -> {
                spec.clientId(Dotenv.load().get("TWITCH_CLIENT_ID"));
                spec.clientSecret(Dotenv.load().get("TWITCH_SECRET"));
                spec.poolShards(4);
                spec.conduitId(existing_conduit_id);
            });
            return true;
        } catch (ConduitNotFoundException e) {
            // TODO create new conduit and recreate subscriptions pulling from database
            Logger.warn(e);
            // create new conduit
            if (!setConduit(null)) {
                Logger.error(e);
                return false;
            }
            // pull subscriptions from database and recreate them
        } catch (CreateConduitException e) {
            Logger.error(e);
        } catch (ShardTimeoutException e) {
            Logger.error(e);
        } catch (ConduitResizeException e) {
            Logger.error(e);
        } catch (ShardRegistrationException e) {
            Logger.error(e);
        }
        return false;
    }

    private void registerEventListeners() {
        IEventManager eventManager = conduit.getEventManager();
        eventManager.onEvent(StreamOnlineEvent.class, System.out::println);
        eventManager.onEvent(EventSocketSubscriptionSuccessEvent.class, System.out::println);
        eventManager.onEvent(EventSocketSubscriptionFailureEvent.class, System.out::println);
    }

    public static TwitchConduit getConduit(long bot_uid) {
        String potentialConduitId = Database.GlobalTable.getConduitId(bot_uid);
        TwitchConduit con = conduit == null ? new TwitchConduit(potentialConduitId) : self;
        Database.GlobalTable.setConduitId(bot_uid, conduit.getConduitId());
        return con;
    }

    /**
     * returns true if successful
     * @param channel_name
     * @return
     */
    public Optional<String> registerSubscription(String channel_name) {
        Optional<Long> channelId = TwitchAPI.getChannelId(channel_name);
        return channelId.isPresent() ? registerSubscription(channelId.get()) : Optional.empty();            
    }

    /**
     * 
     * @param channel_id
     * @return empty if unsuccessful
     */
    public Optional<String> registerSubscription(long channel_id) {
        try {
            EventSubSubscription sub = conduit.register(SubscriptionTypes.STREAM_ONLINE, condition -> condition.broadcasterUserId(""+channel_id).build()).orElseThrow();
            Logger.trace("Twitch subscription registered for channel_id: {}", channel_id);
            return Optional.of(sub.getId());
        } catch (NoSuchElementException e) {
            Logger.debug("Could not create subscrition for channel_id: {}", channel_id);
            return Optional.empty();
        }
    }

    public boolean unregisterSubscription(String sub_id) {
        String authToken = TwitchAuth.appAccessToken;
        if (!TwitchAPI.twitchClient.getHelix().deleteEventSubSubscription(authToken, sub_id).isSuccessfulExecution()) {
            TwitchAuth.refreshAppAccessToken();
            authToken = TwitchAuth.appAccessToken;
        } else {
            return true;
        }
        try {
            TwitchAPI.twitchClient.getHelix().deleteEventSubSubscription(authToken, sub_id).execute();
            return true;
        } catch (Exception e) {
            Logger.error(e);
        }
        return false;
    }

    public String getConduitId() {
        return conduit.getConduitId();
    }
}
