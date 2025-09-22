import java.awt.Color;
import java.util.Optional;

import org.tinylog.Logger;

import com.github.twitch4j.eventsub.EventSubSubscription;
import com.github.twitch4j.eventsub.subscriptions.SubscriptionTypes;

import pingping.Database.Database;
import pingping.Database.Database.TwitchSubsTable;
import pingping.Database.OrmObjects.TwitchSub;
import pingping.Discord.DiscordAPI;
import pingping.Discord.Events.ErrorLogEvent;
import pingping.Discord.Events.TwitchStreamEvent;
import pingping.Discord.Helpers.PushStreamNotification;
import pingping.Exceptions.DatabaseException;
import pingping.Exceptions.InvalidArgumentException;
import pingping.Exceptions.TwitchApiException;
import pingping.Twitch.TwitchAPI;
import pingping.Twitch.TwitchConduit;

@SuppressWarnings("unused")
public class Test {
    public static void main(String[] args) {
        // try {
        //     Database.getConnection();
        //     TwitchConduit.getConduit();
        // } catch (Exception e) {
        //     Logger.error(e, "Failed to start up successfully. Quitting.");
        //     return;
        // }

        System.out.println(TwitchSub.class.getSimpleName());
    }
}
