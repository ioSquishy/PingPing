package pingping;

import pingping.Twitch.TwitchAPI;
import pingping.Twitch.TwitchConduit;

public class Main {
    // public static final DiscordApi api = new DiscordApiBuilder().setToken(Dotenv.load().get("DISCORD_TOKEN")).login().join();
    public static void main(String[] args) {
        Database.connect();
        Database.createBaseTables();

        // get TwitchConduit
        // long bot_userid = 0L;
        // Database.GlobalTable.insertRow(bot_userid); // insert botid if it doesnt exist
        // String potentialConduitId = Database.GlobalTable.getConduitId(bot_userid); // get conduitid or null
        // TwitchConduit conduit = TwitchConduit.getConduit(potentialConduitId); // create or get conduit
        // Database.GlobalTable.setConduitId(bot_userid, conduit.getConduitId()); // store conduitid

        // try subs
        long c_id1 = TwitchAPI.getChannelId("asquishy").get();
        long c_id2 = TwitchAPI.getChannelId("apricot").get();
        long c_id3 = TwitchAPI.getChannelId("nyanners").get();
        System.out.println("cid1: " + c_id1);
        System.out.println("cid2: " + c_id2);
        System.out.println("cid3: " + c_id3);
        System.out.println("insert: " + Database.TwitchSubsTable.insertSubscription(0L, c_id1, 0L, 0L));
        System.out.println("insert: " + Database.TwitchSubsTable.insertSubscription(0L, c_id2, 0L, 0L));
        System.out.println("insert: " + Database.TwitchSubsTable.insertSubscription(0L, c_id3, 0L, 0L));

        System.out.println("subs in table:");
        Database.TwitchSubsTable.pullSubscriptionIds(0L).forEach(str -> {
            System.out.print(str + ",");
        });
    }
}