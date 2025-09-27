package pingping.Database.Tables;

import pingping.Database.OrmObjects.StreamerSubscription;
import pingping.Database.OrmObjects.YoutubeSub;

public class YoutubeSubsTable {
    //TODO
    public static final String tableName = "youtube_subscriptions";
    // columns in YoutubeSub.java

    public static String tableCreationSql() {
        return "CREATE TABLE IF NOT EXISTS " + YoutubeSubsTable.tableName + " (" +
            StreamerSubscription.SERVER_ID + " INTEGER NOT NULL," +
            StreamerSubscription.BROADCASTER_ID + " INTEGER NOT NULL," +
            StreamerSubscription.PINGROLE_ID + " INTEGER NOT NULL," +
            StreamerSubscription.PINGCHANNEL_ID + " INTEGER NOT NULL," +
            YoutubeSub.UPLOADS_PLAYLIST_ID + " STRING NOT NULL," +
            YoutubeSub.BROADCASTER_HANDLE + " STRING," +
            YoutubeSub.LAST_STREAM_VIDEO_ID + " STRING," +
            "PRIMARY KEY ("+StreamerSubscription.SERVER_ID+","+StreamerSubscription.BROADCASTER_ID+")," +
            "FOREIGN KEY ("+StreamerSubscription.SERVER_ID+") REFERENCES "+ServerTable.tableName+"("+ServerTable.Columns.SERVER_ID+") ON DELETE CASCADE" + 
            ");";
    }
}
