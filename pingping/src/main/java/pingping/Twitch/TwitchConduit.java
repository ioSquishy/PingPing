package pingping.Twitch;

import java.util.NoSuchElementException;
import java.util.Optional;

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
    private TwitchConduit(Optional<String> existingConduitId) {
        try {
            conduit = TwitchConduitSocketPool.create(spec -> {
                spec.clientId(Dotenv.load().get("TWITCH_CLIENT_ID"));
                spec.clientSecret(Dotenv.load().get("TWITCH_SECRET"));
                if (existingConduitId.isPresent()) {
                    spec.conduitId(existingConduitId.get());
                } else {
                    spec.poolShards(4);
                }            
            });
        } catch (ConduitNotFoundException e) {
            // TODO create new conduit and recreate subscriptions pulling from database
            e.printStackTrace();
        } catch (CreateConduitException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ConduitResizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ShardTimeoutException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ShardRegistrationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        self = this;
        registerEventListeners();
    }

    private void registerEventListeners() {
        IEventManager eventManager = conduit.getEventManager();
        eventManager.onEvent(StreamOnlineEvent.class, System.out::println);
        eventManager.onEvent(EventSocketSubscriptionSuccessEvent.class, System.out::println);
        eventManager.onEvent(EventSocketSubscriptionFailureEvent.class, System.out::println);
    }

    public static TwitchConduit getConduit(Optional<String> existingConduitId) {
        return conduit == null ? new TwitchConduit(existingConduitId) : self;
    }

    public boolean registerSubscription(String channelName) {
        String channelId = TwitchAPI.getChannelId(channelName);
        try {
            EventSubSubscription sub = conduit.register(SubscriptionTypes.STREAM_ONLINE, b -> b.broadcasterUserId(channelId).build()).orElseThrow();
            System.out.println("sub successful!");
            return true;
        } catch (NoSuchElementException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getConduitId() {
        return conduit.getConduitId();
    }
}
