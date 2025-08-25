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

public class TwitchConduit {
    private static TwitchConduit self = null;
    private static IEventSubConduit conduit;

    /**
     * Creates or retrieves a conduit
     * @param existingConduitId if provided, will search for existing conduit or return a new one
     */
    private TwitchConduit(@Nullable String existingConduitId) {
        if (setConduit(existingConduitId)) {
            self = this;
            registerEventListeners();
        }
    }

    /**
     * return true if successful
     * @param existingConduitId
     * @return
     */
    private static boolean setConduit(@Nullable String existingConduitId) {
        try {
            conduit = TwitchConduitSocketPool.create(spec -> {
                spec.clientId(Dotenv.load().get("TWITCH_CLIENT_ID"));
                spec.clientSecret(Dotenv.load().get("TWITCH_SECRET"));
                spec.poolShards(4);
                spec.conduitId(existingConduitId);
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

    public static TwitchConduit getConduit(@Nullable String existingConduitId) {
        return conduit == null ? new TwitchConduit(existingConduitId) : self;
    }

    /**
     * returns true if successful
     * @param channelName
     * @return
     */
    public boolean registerSubscription(String channelName) {
        Optional<Long> channelId = TwitchAPI.getChannelId(channelName);
        return channelId.isPresent() ? registerSubscription(channelId.get()) : false;            
    }

    public boolean registerSubscription(long channelId) {
        try {
            EventSubSubscription sub = conduit.register(SubscriptionTypes.STREAM_ONLINE, condition -> condition.broadcasterUserId(""+channelId).build()).orElseThrow();
            System.out.println("sub successful!");
            return true;
        } catch (NoSuchElementException e) {
            Logger.debug("Could not create subscrition for channelId: {}", channelId);
            return false;
        }
    }

    public String getConduitId() {
        return conduit.getConduitId();
    }
}
