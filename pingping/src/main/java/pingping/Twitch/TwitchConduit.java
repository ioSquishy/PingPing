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
import pingping.Database.Database;
import pingping.Exceptions.DatabaseException;
import pingping.Exceptions.TwitchApiException;

public class TwitchConduit {
    private static TwitchConduit self = null;
    private IEventSubConduit conduit;

    /**
     * Creates or retrieves a conduit
     * @param existing_conduit_id if provided, will search for existing conduit or return a new one
     * @throws TwitchApiException  if conduit registration is unsuccessful
     * @throws DatabaseException if database connection unsuccessful or fails to store conduit id
     */
    private TwitchConduit(long bot_id) throws TwitchApiException, DatabaseException {
        Logger.warn("Existing TwitchConduit not found; Creating new twitch conduit for bot id: {}", bot_id);
        String potentialConduitId = Database.GlobalTable.getConduitId(bot_id);
        if (potentialConduitId == null) {
            Logger.warn("No existing conduit id in database found for bot id {}", bot_id);
        } else {
            Logger.debug("Potential conduit id for bot id {}: {}", bot_id, potentialConduitId);
        }
        if (setConduit(potentialConduitId)) {
            String actualConduitId = conduit.getConduitId();
            Logger.debug("Setting conduit id for bot id {} to {}", bot_id, actualConduitId);
            Database.GlobalTable.putConduitId(bot_id, actualConduitId);
            Logger.info("Set conduit id for bot id {} to {}", bot_id, actualConduitId);
            self = this;
            registerEventListeners();
            Logger.info("Twitch conduit connected.");
        } else {
            throw new TwitchApiException("Conduit registration unsuccessful.");
        }
    }

    /**
     * return true if successful
     * @param existing_conduit_id
     * @return
     */
    private boolean setConduit(@Nullable String potentialConduitId) {
        Logger.trace("Running setConduit with potential id: {}", potentialConduitId);
        try {
            conduit = TwitchConduitSocketPool.create(spec -> {
                spec.clientId(Dotenv.load().get("TWITCH_CLIENT_ID"));
                spec.clientSecret(Dotenv.load().get("TWITCH_SECRET"));
                spec.poolShards(4);
                spec.conduitId(potentialConduitId);
            });
            Logger.trace("Conduit created with id: {}", conduit.getConduitId());
            return true;
        } catch (ConduitNotFoundException e) {
            // TODO create new conduit and recreate subscriptions pulling from database
            Logger.warn(e, "Conduit with id {} not found. Creating new conduit...", potentialConduitId);
            // create new conduit
            if (!setConduit(null)) {
                Logger.error("Failed to create new conduit.");
                return false;
            }
            Logger.info("New conduit created with id {}", conduit.getConduitId());
            // pull subscriptions from database and recreate them
            
            return true;
        } catch (CreateConduitException | ShardTimeoutException | ConduitResizeException | ShardRegistrationException e) {
            Logger.error(e);
        }
        return false;
    }

    private void registerEventListeners() {
        Logger.info("Registered event listeners for conduit {}", conduit.getConduitId());
        IEventManager eventManager = conduit.getEventManager();
        eventManager.onEvent(StreamOnlineEvent.class, System.out::println);
        eventManager.onEvent(EventSocketSubscriptionSuccessEvent.class, System.out::println);
        eventManager.onEvent(EventSocketSubscriptionFailureEvent.class, System.out::println);
    }

    /**
     * 
     * @param bot_id
     * @return
     * @throws TwitchApiException if conduit registration unsuccessful
     * @throws DatabaseException if database connection unsuccessful or fails to store conduit id
     */
    public static TwitchConduit getConduit(long bot_id) throws TwitchApiException, DatabaseException {
        TwitchConduit con = self == null ? new TwitchConduit(bot_id) : self;
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
    public Optional<String> registerSubscription(long broadcaster_id) {
        try {
            EventSubSubscription sub = conduit.register(SubscriptionTypes.STREAM_ONLINE, condition -> condition.broadcasterUserId(""+broadcaster_id).build()).orElseThrow();
            Logger.trace("Twitch subscription registered for broadcaster_id: {}", broadcaster_id);
            return Optional.of(sub.getId());
        } catch (NoSuchElementException e) {
            Logger.debug("Could not create subscription for broadcaster_id: {}", broadcaster_id);
            return Optional.empty();
        }
    }

    /**
     * 
     * @param eventsub_id
     * @return true if deregistration was successful
     */
    public boolean unregisterSubscription(String eventsub_id) {
        String authToken = TwitchAuth.appAccessToken;
        if (!TwitchAPI.twitchClient.getHelix().deleteEventSubSubscription(authToken, eventsub_id).isSuccessfulExecution()) {
            TwitchAuth.refreshAppAccessToken();
            authToken = TwitchAuth.appAccessToken;
        } else {
            return true;
        }
        try {
            TwitchAPI.twitchClient.getHelix().deleteEventSubSubscription(authToken, eventsub_id).execute();
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
