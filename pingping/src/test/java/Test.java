import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Discord.Events.TwitchStreamEvent;
import pingping.Exceptions.DatabaseException;
import pingping.Twitch.TwitchAPI;
import pingping.Twitch.TwitchConduit;

@SuppressWarnings("unused")
public class Test {
    public static void main(String[] args) {
        try {
            Database.getConnection();
            TwitchConduit.getConduit();
        } catch (Exception e) {
            Logger.error(e, "Failed to start up successfully. Quitting.");
            return;
        }

        try {
            TwitchStreamEvent.handleStreamOnlineEvent(82350088L);
        } catch (DatabaseException e) {
            e.printStackTrace();
        }

        // TwitchAPI.deleteAllExistingConduits();
        // TwitchAPI.getEventSubscriptions().forEach(sub -> {
        //     System.out.println(sub.getId());
        // });
    }
}
