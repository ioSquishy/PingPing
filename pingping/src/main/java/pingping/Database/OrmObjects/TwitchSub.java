package pingping.Database.OrmObjects;

public class TwitchSub {
    public final long server_id;
    public final long broadcaster_id;
    public final String eventsub_id;
    public final long pingrole_id;
    public final long pingchannel_id;

    public static enum Columns {
        SERVER_ID("server_id", null), // INTEGER
        BROADCASTER_ID("broadcaster_id", "streamer"), // INTEGER
        EVENTSUB_ID("eventsub_id", null), // STRING
        PINGROLE_ID("pingrole_id", "role"), // INTEGER
        PINGCHANNEL_ID("pingchannel_id", "channel"); // INTEGER

        public final String sql_column_name;
        public final String dcmd_argument_name;
        private Columns(String sql_column_name, String discord_command_argument_name) {
            this.sql_column_name = sql_column_name;
            this.dcmd_argument_name = discord_command_argument_name;
        }
        @Override
        public String toString() {
            return this.sql_column_name;
        }
    }

    public TwitchSub(long server_id, long broadcaster_id, String eventsub_id, long pingrole_id, long pingchannel_id) {
        this.server_id = server_id;
        this.broadcaster_id = broadcaster_id;
        this.eventsub_id = eventsub_id;
        this.pingrole_id = pingrole_id;
        this.pingchannel_id = pingchannel_id;
    }
}
