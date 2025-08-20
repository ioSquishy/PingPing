package pingping.Twitch;

import java.util.List;
import java.util.NoSuchElementException;

import com.github.philippheuer.events4j.api.IEventManager;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
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
import com.github.twitch4j.helix.domain.UserList;

import io.github.cdimascio.dotenv.Dotenv;

public class TwitchAPI {
    private static final TwitchClient twitchClient = TwitchClientBuilder.builder()
        .withClientId(Dotenv.load().get("TWITCH_CLIENT_ID"))
        .withClientSecret(Dotenv.load().get("TWITCH_SECRET"))
        .withEnableHelix(true)
        .build();

    public static TwitchAPI.Conduit createConduit() {
        try {
            IEventSubConduit conduit = TwitchConduitSocketPool.create(spec -> {
                spec.clientId(Dotenv.load().get("TWITCH_CLIENT_ID"));
                spec.clientSecret(Dotenv.load().get("TWITCH_SECRET"));
                spec.poolShards(4);
            });
            return new Conduit(conduit);
        } catch (ShardTimeoutException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (CreateConduitException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ConduitNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ConduitResizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ShardRegistrationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public static String getChannelId(String channelName) {
        UserList users = twitchClient.getHelix().getUsers(null, null, List.of(channelName)).execute();
        if (users.getUsers().isEmpty()) {
            System.err.println("Error: Could not resolve username to channel ID for channelName: " + channelName);
            return null;
        }

        return users.getUsers().get(0).getId();
    }

    public static class Conduit {
        private final IEventSubConduit conduit;
        private Conduit(IEventSubConduit con) {
            this.conduit = con;
            registerEventListeners();
        }

        private void registerEventListeners() {
            IEventManager eventManager = conduit.getEventManager();
            eventManager.onEvent(StreamOnlineEvent.class, System.out::println);
            eventManager.onEvent(EventSocketSubscriptionSuccessEvent.class, System.out::println);
            eventManager.onEvent(EventSocketSubscriptionFailureEvent.class, System.out::println);
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
    }
}

