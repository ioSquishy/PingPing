package pingping.Database.OrmObjects;

/**
 * Informaton for a Youtube channel independent of server subscriptions
 * Not physically related to Youtube Sub, main purpose is for use in Youtube.LivePoller to check if broadcaster is streaming with minimal info
 */
public class YoutubeChannel {
    public final String broadcaster_id;
    public final String uploads_playlist_id;
    public final String broadcaster_handle;
    public final String last_stream_video_id;

    public YoutubeChannel(String broadcaster_id, String uploads_playlist_id, String broadcaster_handle, String last_stream_video_id) {
        this.broadcaster_id = broadcaster_id;
        this.uploads_playlist_id = uploads_playlist_id;
        this.broadcaster_handle = broadcaster_handle;
        this. last_stream_video_id = last_stream_video_id;
    }
}
