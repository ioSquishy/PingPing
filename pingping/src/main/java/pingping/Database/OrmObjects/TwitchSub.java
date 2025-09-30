package pingping.Database.OrmObjects;

public class TwitchSub extends StreamerSubscription {
    public static final SubscriptionColumn EVENTSUB_ID = new SubscriptionColumn("eventsub_id", null); // STRING

    public final String eventsub_id;

    public TwitchSub(long server_id, String broadcaster_id, long pingrole_id, long pingchannel_id, String eventsub_id) {
        super(server_id, broadcaster_id, pingrole_id, pingchannel_id);
        this.eventsub_id = eventsub_id;
    }
}
