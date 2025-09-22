package pingping.Twitch;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;

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
import pingping.Main;
import pingping.Database.Database;
import pingping.Exceptions.DatabaseException;
import pingping.Exceptions.InvalidArgumentException;
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
        Logger.warn("Existing TwitchConduit instance not found; Initializing new instance for bot id: {}", bot_id);
        String potentialConduitId = Database.GlobalTable.getConduitId(bot_id);
        if (potentialConduitId == null) {
            Logger.warn("No existing conduit id in database found for bot id {}", bot_id);
            if (Database.GlobalTable.getNumberOfConduits() >= 5) {
                throw new DatabaseException("5 conduits already exist! Cannot create more.");
            }
        } else {
            Logger.debug("Potential conduit id for bot id {}: {}", bot_id, potentialConduitId);
        }
        if (setConduit(potentialConduitId)) {
            String actualConduitId = conduit.getConduitId();
            Logger.debug("Setting conduit id for bot id {} to {}", bot_id, actualConduitId);
            Database.GlobalTable.putConduitId(bot_id, actualConduitId);
            Logger.debug("Set conduit id for bot id {} to {}", bot_id, actualConduitId);
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
                List<Long> subBroadcasterIds = Database.TwitchSubsTable.pullSubscriptionBroadcasterIds();
                for (Long id : subBroadcasterIds) {
                    try {
                        String newEventSubId = registerSubscription(id);
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
        return getConduit(Main.INSTANCE_ID);
    }

    /**
     * @throws TwitchApiException if conduit registration unsuccessful
     * @throws DatabaseException if database connection unsuccessful or fails to store conduit id
     */
    private static TwitchConduit getConduit(byte instance_id) throws TwitchApiException, DatabaseException {
        TwitchConduit con = self == null ? new TwitchConduit(instance_id) : self;
        return con;
    }

    /**
     * @param channel_name
     * @return event sub id
     * @throws TwitchApiException if api request fails
     * @throws InvalidArgumentException if channel id could not be found
     */
    public String registerSubscription(String channel_name) throws TwitchApiException, InvalidArgumentException {
        long channelId = TwitchAPI.getChannelId(channel_name);
        return registerSubscription(channelId);            
    }

    /**
     * Registers a STREAM_ONLINE subscription for the specified broadcaster id.
     * If a subscription already exists, the method will return the existing eventSubId. Otherwise, a new subscription is created.
     * @param broadcaster_id to create STREAM_ONLINE subscription for
     * @return event sub id of new or existing subscription
     * @throws TwitchApiException if registration failed
     */
    public String registerSubscription(long broadcaster_id) throws TwitchApiException {
        try {
            Optional<EventSubSubscription> existingSub = TwitchAPI.getEnabledEventSubscriptions(Long.toString(broadcaster_id)).stream().filter(sub -> sub.getRawType().equals(SubscriptionTypes.STREAM_ONLINE.getName())).findAny();
            if (existingSub.isPresent()) {
                return existingSub.get().getId();
            } else {
                EventSubSubscription sub = conduit.register(SubscriptionTypes.STREAM_ONLINE, condition -> condition.broadcasterUserId(""+broadcaster_id).build()).orElseThrow();
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
        return TwitchAPI.unregisterEventSubscription(eventsub_id);
    }

    public String getConduitId() {
        return conduit.getConduitId();
    }
}
