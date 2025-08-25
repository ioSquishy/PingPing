package pingping;

import pingping.Twitch.TwitchAPI;
import pingping.Twitch.TwitchConduit;

public class Main {
    // public static final DiscordApi api = new DiscordApiBuilder().setToken(Dotenv.load().get("DISCORD_TOKEN")).login().join();
    public static void main(String[] args) {
        Database.connect();
        Database.createBaseTables();

        // get TwitchConduit
        long bot_userid = 0L;
        Database.GlobalTable.insertRow(bot_userid); // insert botid if it doesnt exist
        String potentialConduitId = Database.GlobalTable.getConduitId(bot_userid); // get conduitid or null
        TwitchConduit conduit = TwitchConduit.getConduit(potentialConduitId); // create or get conduit
        Database.GlobalTable.setConduitId(bot_userid, conduit.getConduitId()); // store conduitid

        // try subs
        long c_id = TwitchAPI.getChannelId("asquishy").get();
        System.out.println("insert: " + Database.TwitchSubsTable.insertSubscription(0L, c_id, 0L, 0L));

        System.out.println("subs in table:");
        Database.TwitchSubsTable.pullSubscriptionIds(0L).forEach(str -> {
            System.out.print(str + ",");
        });
    }
}