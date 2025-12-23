package pingping.Database.Tables;

import org.tinylog.Logger;

import pingping.Database.OrmObjects.StreamerSubscription;
import pingping.Database.OrmObjects.YoutubeSub;

public class YoutubeChannelsTable {
    public static final String tableName = "youtube_channels";
    // columns in YoutubeSub.java

    public static String tableCreationSql() {
        String sql = "CREATE TABLE IF NOT EXISTS " + YoutubeChannelsTable.tableName + " (" +
            StreamerSubscription.BROADCASTER_ID + " STRING NOT NULL," +
            YoutubeSub.UPLOADS_PLAYLIST_ID + " STRING NOT NULL," +
            YoutubeSub.BROADCASTER_HANDLE + " STRING NOT NULL," +
            YoutubeSub.LAST_STREAM_VIDEO_ID + " STRING," +
            "PRIMARY KEY ("+StreamerSubscription.BROADCASTER_ID+")" +
            ");";
        Logger.trace("YoutubeChannelsTable table create SQL: {}", sql);
        return sql;
    }
}
