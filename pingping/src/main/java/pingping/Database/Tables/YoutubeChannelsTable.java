package pingping.Database.Tables;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;

import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Database.OrmObjects.YoutubeSub;
import pingping.Exceptions.DatabaseException;

public class YoutubeChannelsTable {
    public static final String tableName = "youtube_channels";
    // columns in YoutubeSub.java

    public static String tableCreationSql() {
        // language=sql
        final String sql = MessageFormat.format("""
            CREATE TABLE IF NOT EXISTS {0} (
                {1} TEXT UNIQUE NOT NULL, -- broadcaster_id
                {2} TEXT NOT NULL, -- broadcaster_handle
                {3} TEXT NOT NULL, -- uploads_playlist_id
                {4} TEXT, -- last_stream_vid_id
                PRIMARY KEY ({1})
                )
                """, YoutubeChannelsTable.tableName, YoutubeSub.BROADCASTER_ID, YoutubeSub.BROADCASTER_HANDLE,
                    YoutubeSub.UPLOADS_PLAYLIST_ID, YoutubeSub.LAST_STREAM_VIDEO_ID);
        Logger.trace("YoutubeChannelsTable table create SQL: {}", sql);
        return sql;
    }

    protected static void insertChannel(@NotNull String broadcaster_id, @NotNull String broadcaster_handle, @NotNull String uploads_playlist_id, String last_stream_vid_id) throws DatabaseException {
        // language=sql
        final String sql = "INSERT OR IGNORE INTO youtube_channels (broadcaster_id, broadcaster_handle, uploads_playlist_id, last_stream_vid_id) VALUES (?,?,?,?)";
        Logger.trace("SQL: {}\n?: {}, {}, {}, {}", sql, broadcaster_id, broadcaster_handle, uploads_playlist_id, last_stream_vid_id);

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setString(1, broadcaster_id);
            statement.setString(2, broadcaster_handle);
            statement.setString(3, uploads_playlist_id);
            statement.setString(4, last_stream_vid_id);
            statement.executeUpdate();
        } catch (SQLException e) {
            Logger.error(e, "Failed to insert broadcaster_id {} into database.", broadcaster_id);
            throw new DatabaseException("Failed to insert channel into database.");
        }
    }

    protected static void setLastStreamVideoId(@NotNull String broadcaster_id, @NotNull String last_stream_video_id) throws DatabaseException {
        final String sql = "UPDATE youtube_channels SET last_stream_vid_id = ? WHERE broadcaster_id = ?";
        Logger.trace("SQL: {}\n?: {}", last_stream_video_id, broadcaster_id);

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setString(1, last_stream_video_id);
            statement.setString(2, broadcaster_id);
            statement.executeUpdate();
            Logger.debug("Updated last_stream_video_id in database for broadcasters with id {} to {}", broadcaster_id, last_stream_video_id);
        } catch (SQLException e) {
            Logger.error(e, "Failed to update last_stream_video_id in database for broadcasters with id {} to {}", broadcaster_id, last_stream_video_id);
            throw new DatabaseException("Failed to update last_stream_video_id in database.");
        }
    }

    // TODO call from UnregisterYoutubeSub.java
    public static void removeChannel(@NotNull String broadcaster_id) throws DatabaseException {
        final String sql = "DELETE FROM youtube_channels WHERE broadcaster_id = ?";
        Logger.trace("SQL: {}\n?: {}", sql, broadcaster_id);

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setString(1, broadcaster_id);
            statement.executeUpdate();
            Logger.debug("Removed youtube channel from database for broadcaster with id {}", broadcaster_id);
        } catch (SQLException e) {
            Logger.error(e, "Failed to remove youtube channel from database for broadcasters with id {}", broadcaster_id);
            throw new DatabaseException("Failed to remove youtube channel from database.");
        }
    }
}
