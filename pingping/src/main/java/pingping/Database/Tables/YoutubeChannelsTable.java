package pingping.Database.Tables;

import java.text.MessageFormat;

import org.tinylog.Logger;

import pingping.Database.OrmObjects.YoutubeSub;

public class YoutubeChannelsTable {
    public static final String tableName = "youtube_channels";
    // columns in YoutubeSub.java

    public static String tableCreationSql() {
        // language=sql
        final String sql = MessageFormat.format("""
            CREATE TABLE IF NOT EXISTS {0} (
                {1} TEXT UNIQUE NOT NULL, -- broadcaster_id
                {2} TEXT NOT NULL, -- uploads_playlist_id
                {3} TEXT NOT NULL, -- broadcaster_handle
                {4} TEXT, -- last_stream_vid_id
                PRIMARY KEY ({1})
                )
                """, YoutubeChannelsTable.tableName, YoutubeSub.BROADCASTER_ID, YoutubeSub.UPLOADS_PLAYLIST_ID,
                    YoutubeSub.BROADCASTER_HANDLE, YoutubeSub.LAST_STREAM_VIDEO_ID);
        Logger.trace("YoutubeChannelsTable table create SQL: {}", sql);
        return sql;
    }
}
