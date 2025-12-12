package pingping.Database.OrmObjects;

import pingping.Database.OrmObjects.StreamerSubscription.SubscriptionColumn;

public class YoutubeChannel {
    public static final SubscriptionColumn UPLOADS_PLAYLIST_ID = new SubscriptionColumn("uploads_playlist_id", null); // STRING
    public static final SubscriptionColumn BROADCASTER_HANDLE = new SubscriptionColumn("broadcaster_handle", "Streamer"); // STRING
    public static final SubscriptionColumn LAST_STREAM_VIDEO_ID = new SubscriptionColumn("last_stream_vid_id", null); // STRING

    /**
     * YoutubeSub.SERVER_ID+","+YoutubeSub.BROADCASTER_ID+","+YoutubeSub.PINGROLE_ID+","+YoutubeSub.PINGCHANNEL_ID+","+YoutubeSub.UPLOADS_PLAYLIST_ID+","+YoutubeSub.BROADCASTER_HANDLE+","+YoutubeSub.LAST_STREAM_VIDEO_ID
     */
    public static final String ALL_COLUMNS = YoutubeChannel.BROADCASTER_HANDLE+","+YoutubeChannel.UPLOADS_PLAYLIST_ID+","+YoutubeChannel.BROADCASTER_HANDLE+","+YoutubeChannel.LAST_STREAM_VIDEO_ID;

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
