package pingping.Database.OrmObjects;

public class StreamerSubscription {
    // static
    public static class SubscriptionColumn {
        public final String SQL_COLUMN;
        public final String DISCORD_CMD_ARG;

        public SubscriptionColumn(String sqlColumnName, String dcmdArgumentName) {
            this.SQL_COLUMN = sqlColumnName;
            this.DISCORD_CMD_ARG = dcmdArgumentName;
        }

        @Override
        public String toString() {
            return this.SQL_COLUMN;
        }
    }

    public static final SubscriptionColumn SERVER_ID = new SubscriptionColumn("server_id", null);  // INTEGER
    public static final SubscriptionColumn BROADCASTER_ID = new SubscriptionColumn("broadcaster_id", "streamer");  // STRING
    public static final SubscriptionColumn PINGROLE_ID = new SubscriptionColumn("pingrole_id", "role"); // INTEGER
    public static final SubscriptionColumn PINGCHANNEL_ID = new SubscriptionColumn("pingchannel_id", "channel"); // INTEGER

    // instance
    public final long server_id;
    public final String broadcaster_id;
    public final long pingrole_id;
    public final long pingchannel_id;

    public StreamerSubscription(long server_id, String broadcaster_id, long pingrole_id, long pingchannel_id) {
        this.server_id = server_id;
        this.broadcaster_id = broadcaster_id;
        this.pingrole_id = pingrole_id;
        this.pingchannel_id = pingchannel_id;
    }

    
}
