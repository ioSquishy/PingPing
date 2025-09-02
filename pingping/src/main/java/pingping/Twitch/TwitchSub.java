package pingping.Twitch;

public class TwitchSub {
    public final long server_id;
    public final long broadcaster_id;
    public final String eventsub_id;
    public final long pingrole_id;
    public final long pingchannel_id;

    public TwitchSub(long server_id, long broadcaster_id, String eventsub_id, long pingrole_id, long pingchannel_id) {
        this.server_id = server_id;
        this.broadcaster_id = broadcaster_id;
        this.eventsub_id = eventsub_id;
        this.pingrole_id = pingrole_id;
        this.pingchannel_id = pingchannel_id;
    }
}
