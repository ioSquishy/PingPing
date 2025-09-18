import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Database.Database.TwitchSubsTable;
import pingping.Database.OrmObjects.TwitchSub;
import pingping.Discord.DiscordAPI;
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
        try {
            Database.getConnection();
            DiscordAPI.connect();
            TwitchConduit.getConduit();
        } catch (Exception e) {
            Logger.error(e, "Failed to start up successfully. Quitting.");
            return;
        }

        try {
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        // TwitchAPI.deleteAllExistingConduits();
        // TwitchAPI.getEventSubscriptions().forEach(sub -> {
        //     System.out.println(sub.getId());
        // });
    }
}
