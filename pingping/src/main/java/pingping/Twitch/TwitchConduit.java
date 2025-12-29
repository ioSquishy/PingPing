package pingping.Twitch;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import com.github.twitch4j.eventsub.EventSubSubscription;
import com.github.twitch4j.eventsub.events.StreamOnlineEvent;
import com.github.twitch4j.eventsub.socket.IEventSubConduit;
import com.github.twitch4j.eventsub.socket.conduit.TwitchConduitSocketPool;
import com.github.twitch4j.eventsub.socket.conduit.exceptions.ConduitNotFoundException;
import com.github.twitch4j.eventsub.socket.conduit.exceptions.ConduitResizeException;
import com.github.twitch4j.eventsub.socket.conduit.exceptions.CreateConduitException;
import com.github.twitch4j.eventsub.socket.conduit.exceptions.ShardRegistrationException;
import com.github.twitch4j.eventsub.socket.conduit.exceptions.ShardTimeoutException;
import com.github.twitch4j.eventsub.subscriptions.SubscriptionTypes;

import io.github.cdimascio.dotenv.Dotenv;
import pingping.Database.Database;
import pingping.Exceptions.DatabaseException;
import pingping.Exceptions.InvalidArgumentException;
import pingping.Exceptions.TwitchApiException;

public class TwitchConduit {
    private static TwitchConduit self = null;
    private IEventSubConduit conduit;
    private final String GLOBAL_TABLE_CONDUIT_KEY = "conduit_id";

    /**
     * Creates or retrieves a conduit
     * @param existing_conduit_id if provided, will search for existing conduit or return a new one
     * @throws TwitchApiException  if conduit registration is unsuccessful
     * @throws DatabaseException if database connection unsuccessful or fails to store conduit id
     */
    private TwitchConduit() throws TwitchApiException, DatabaseException {
        Logger.warn("Existing TwitchConduit instance not found; Initializing new instance.");
        String potentialConduitId = Database.GlobalTable.getValue(GLOBAL_TABLE_CONDUIT_KEY);
        if (potentialConduitId == null) {
            Logger.warn("No existing conduit id in database found.");
        } else {
            Logger.debug("Potential conduit id: {}", potentialConduitId);
        }
        if (setConduit(potentialConduitId)) {
            String actualConduitId = conduit.getConduitId();
            Logger.debug("Setting conduit id to {}", actualConduitId);
            Database.GlobalTable.putValue(GLOBAL_TABLE_CONDUIT_KEY, actualConduitId);
            Logger.debug("Set conduit id to {}", actualConduitId);
            self = this;
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
            Logger.warn(e, "Conduit with id {} not found. Creating new conduit.", potentialConduitId);
            // create new conduit
            if (!setConduit(null)) {
                Logger.error("Failed to create new conduit.");
                return false;
            }
            Logger.debug("New conduit created with id {}", conduit.getConduitId());
            
            // pull subscriptions from database and recreate them
            Logger.trace("Recreating subscriptions in database for new conduit...");
            boolean subsAllRecreated = true;
            try {
                List<String> subBroadcasterIds = Database.TwitchSubsTable.pullSubscriptionBroadcasterIds();
                for (String id : subBroadcasterIds) {
                    try {
                        String newEventSubId = registerSubscriptionById(id);
                        Database.TwitchSubsTable.setEventSubId(id, newEventSubId);
                        Logger.trace("Re-registered subscription for broadcaster {}", id);
                    } catch (TwitchApiException | DatabaseException e1) {
                        subsAllRecreated = false;
                        Logger.error(e1, "Failed to register subscription for broadcaster id {}", id);
                        break;
                    }
                }
            } catch (DatabaseException e1) {
                subsAllRecreated = false;
                Logger.error(e1, "Failed to create new conduit.");
            }

            if (subsAllRecreated) {
                Logger.debug("Successfully recreated conduit and event subscriptions.");
                return true;
            } else {
                Logger.error("Failed to recreate event subscriptions for conduit.");
                TwitchAPI.deleteConduit(conduit.getConduitId());
                return false;
            }
        } catch (CreateConduitException | ShardTimeoutException | ConduitResizeException | ShardRegistrationException e) {
            Logger.error(e);
        }
        return false;
    }

    /**
     * in milliseconds
     * @return -1 if unknown or not connected
     */
    public static long getLatency() {
        return self != null ? self.conduit.getLatency() : -1;
    }

    public void subscribeToStreamOnlineEvents(Consumer<StreamOnlineEvent> consumer) {
        conduit.getEventManager().onEvent(StreamOnlineEvent.class, consumer);
        Logger.trace("Registered StreamOnlineEvent listener for conduit {}", conduit.getConduitId());
    }


    /**
     * @throws TwitchApiException if conduit registration unsuccessful
     * @throws DatabaseException if database connection unsuccessful or fails to store conduit id
     */
    public static TwitchConduit getConduit() throws TwitchApiException, DatabaseException {
        TwitchConduit con = self == null ? new TwitchConduit() : self;
        return con;
    }

    /**
     * @param channel_name
     * @return event sub id
     * @throws TwitchApiException if api request fails
     * @throws InvalidArgumentException if channel id could not be found
     */
    public String registerSubscription(@NotNull String channel_name) throws TwitchApiException, InvalidArgumentException {
        String channelId = TwitchAPI.getChannelId(channel_name);
        return registerSubscriptionById(channelId);            
    }

    /**
     * Registers a STREAM_ONLINE subscription for the specified broadcaster id.
     * If a subscription already exists, the method will return the existing eventSubId. Otherwise, a new subscription is created.
     * @param broadcaster_id to create STREAM_ONLINE subscription for
     * @return event sub id of new or existing subscription
     * @throws TwitchApiException if registration failed
     */
    public String registerSubscriptionById(@NotNull String broadcaster_id) throws TwitchApiException {
        try {
            Optional<EventSubSubscription> existingSub = TwitchAPI.getEnabledEventSubscriptions(broadcaster_id).stream().filter(sub -> sub.getRawType().equals(SubscriptionTypes.STREAM_ONLINE.getName())).findAny();
            if (existingSub.isPresent()) {
                return existingSub.get().getId();
            } else {
                EventSubSubscription sub = conduit.register(SubscriptionTypes.STREAM_ONLINE, condition -> condition.broadcasterUserId(broadcaster_id).build()).orElseThrow();
                Logger.trace("Twitch subscription registered for broadcaster_id: {}", broadcaster_id);
                return sub.getId();
            }
        } catch (NoSuchElementException e) {
            Logger.debug("Could not create subscription for broadcaster_id: {}", broadcaster_id);
            throw new TwitchApiException("Subscription registration through Twitch API unsuccessful.");
        }
    }

    /**
     * Unregister through helix api because to register through conduit directly requires the actual EventSub object
     * @param eventsub_id
     * @return true if deregistration was successful
     */
    public boolean unregisterSubscription(String eventsub_id) {
        // TODO remove TwitchSubsTable entry from database
        // TODO pull number of subs remaining in database for this eventsub_id, if 0, call unregisterEventSubscription
        return TwitchAPI.unregisterEventSubscription(eventsub_id);
    }

    public String getConduitId() {
        return conduit.getConduitId();
    }
}
