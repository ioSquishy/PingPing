package pingping.Twitch;

import java.util.List;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.ITwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.eventsub.EventSubSubscription;
import com.github.twitch4j.eventsub.EventSubTransport;
import com.github.twitch4j.eventsub.EventSubTransportMethod;
import com.github.twitch4j.eventsub.socket.IEventSubSocket;
import com.github.twitch4j.eventsub.socket.events.EventSocketDeleteSubscriptionFailureEvent;
import com.github.twitch4j.eventsub.socket.events.EventSocketDeleteSubscriptionSuccessEvent;
import com.github.twitch4j.eventsub.socket.events.EventSocketSubscriptionFailureEvent;
import com.github.twitch4j.eventsub.socket.events.EventSocketSubscriptionSuccessEvent;
import com.github.twitch4j.eventsub.subscriptions.SubscriptionTypes;
import com.github.twitch4j.helix.domain.UserList;

import io.github.cdimascio.dotenv.Dotenv;

public class TwitchAPI {
    public static ITwitchClient twitchClient = null;
    public static IEventSubSocket eventSocket = null;

    public static void InitClient(String twitchUserAccessToken) {
        twitchClient = TwitchClientBuilder.builder()
            .withEnableEventSocket(true)
            .withEnableHelix(true)
            .withDefaultAuthToken(new OAuth2Credential("twitch", twitchUserAccessToken))
            .withClientId("wnifut61y7rq4iria050d36sni9fp6")
            .withClientSecret(Dotenv.load().get("TWITCH_SECRET"))
            .build();

        eventSocket = twitchClient.getEventSocket();
        // twitchClient.getClientHelper().enableStreamEventListener("twitch4j");

        // init meta-event listeners
        eventSocket.getEventManager().onEvent(EventSocketSubscriptionSuccessEvent.class, System.out::println);
        eventSocket.getEventManager().onEvent(EventSocketSubscriptionFailureEvent.class, event -> {
            System.err.println("fail");
        });
        eventSocket.getEventManager().onEvent(EventSocketDeleteSubscriptionSuccessEvent.class, System.out::println);
        eventSocket.getEventManager().onEvent(EventSocketDeleteSubscriptionFailureEvent.class, System.err::println);

        eventSocket.getEventManager().onEvent(ChannelGoLiveEvent.class, event -> {
            System.out.println("[" + event.getChannel().getName() + "] went live with title " + event.getStream().getTitle() + "!");
        });
    }

    private static boolean IsInitialized() {
        if (twitchClient != null) {
            return true;
        } else {
            System.err.println("Twitch client not yet initialized!");
            return false;
        }
    }

    private static String GetChannelId(String channelName) {
        UserList users = twitchClient.getHelix().getUsers(null, null, List.of(channelName)).execute();
        if (users.getUsers().isEmpty()) {
            System.err.println("Error: Could not resolve username to channel ID for channelName: " + channelName);
            return null;
        }

        return users.getUsers().get(0).getId();
    }

    public static EventSubSubscription RegisterStreamOnlineNotif(String channelName) {
        String channelID = GetChannelId(channelName);
        if (channelID == null) {
            return null;
        }

        EventSubSubscription subscription = SubscriptionTypes.STREAM_ONLINE.prepareSubscription(
            builder -> builder.broadcasterUserId(channelID).build(),
            EventSubTransport.builder().method(EventSubTransportMethod.WEBSOCKET).build()
        );

        if (!eventSocket.register(subscription)) {
            System.err.println("Subscription unsucessfull for channelID: " + channelID);
        }
        return subscription;
    }

    /**
     * 
     * @param subscription
     * @return true if subscription was previously present (and unsubscribed from)
     */
    public static boolean UnregisterSubscription(EventSubSubscription subscription) {
        return eventSocket.unregister(subscription);
    }
}
