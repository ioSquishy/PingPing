package pingping.Database.Tables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Database.OrmObjects.YoutubeSub;
import pingping.Exceptions.DatabaseException;

public class YoutubeSubsTable {
    public static final String tableName = "youtube_subscriptions";
    // columns in YoutubeSub.java

    public static String tableCreationSql() {
        // language=sql
        String sql = MessageFormat.format("""
                CREATE TABLE IF NOT EXISTS {0} (
                    {1} INTEGER UNIQUE NOT NULL, -- server_id
                    {2} TEXT NOT NULL, -- broadcaster_id
                    {3} INTEGER NOT NULL, -- pingrole_id
                    {4} INTEGER NOT NULL, -- pingchannel_id
                    PRIMARY KEY ({1}, {2}),
                    FOREIGN KEY ({1}) REFERENCES {5}({6}) ON DELETE CASCADE,
                    FOREIGN KEY ({2}) REFERENCES {7}({2}) ON DELETE CASCADE
                )
                """, YoutubeSubsTable.tableName, YoutubeSub.SERVER_ID, YoutubeSub.BROADCASTER_ID,
                    YoutubeSub.PINGROLE_ID, YoutubeSub.PINGCHANNEL_ID,
                    ServerTable.tableName, ServerTable.Columns.SERVER_ID,
                    YoutubeChannelsTable.tableName);
        Logger.trace("Youtube table create SQL: {}", sql);
        return sql;
    }

    public static void insertSubscription(@NotNull YoutubeSub sub) throws DatabaseException {
        insertSubscription(sub.server_id, sub.broadcaster_id, sub.pingrole_id, sub.pingchannel_id, sub.uploads_playlist_id, sub.broadcaster_handle, sub.last_stream_video_id);
    }

    public static void insertSubscription(long server_id, @NotNull String broadcaster_id, long pingrole_id, long pingchannel_id, @NotNull String uploads_playlist_id, @NotNull String broadcaster_handle, String last_stream_video_id) throws DatabaseException {
        // language=sql
        final String sql = """
                INSERT OR IGNORE INTO youtube_subscriptions (
                    server_id, broadcaster_id, pingrole_id, pingchannel_id
                ) VALUES (?, ?, ?, ?)
                """;
        Logger.trace("SQL: {}\n?: {}, {}, {}, {}", sql, server_id, broadcaster_id, pingrole_id, pingchannel_id);

        Connection databaseConnection = Database.getConnection();
        try (PreparedStatement statement = databaseConnection.prepareStatement(sql)) {
            Database.ServerTable.insertEntry(server_id);
            statement.setLong(1, server_id);
            statement.setString(2, broadcaster_id);
            statement.setLong(3, pingrole_id);
            statement.setLong(4, pingchannel_id);

            databaseConnection.setAutoCommit(false); // set auto commit to false so i can rollback if first call doesnt succeed
            YoutubeChannelsTable.insertChannel(broadcaster_id, broadcaster_handle, uploads_playlist_id, last_stream_video_id);
            statement.executeUpdate();
            databaseConnection.commit();
        } catch (SQLException e) {
            Logger.error(e, "Failed to insert subscription with server_id {} and broadcaster_id {} into {} table.", server_id, broadcaster_id, tableName);
            throw new DatabaseException("Failed to insert subscription into database.");
        } finally {
            try {
                Database.getConnection().setAutoCommit(true);
            } catch (SQLException e) {
                Logger.error(e, "Failed to set database auto commit to true.");
                throw new DatabaseException("Failed to set database auto commit to true.");
            }
        }
    }

    /**
     * returns YoutubeSub object from current iteration of resultSet
     * does not increment ResultSet index
     * @param result_set assumes the following columns are present (not order-dependent): 
     * @throws SQLException
     */
    private static YoutubeSub createSubFromResultSet(ResultSet result_set) throws SQLException {
        return new YoutubeSub(
            result_set.getLong(YoutubeSub.SERVER_ID.SQL_COLUMN), 
            result_set.getString(YoutubeSub.BROADCASTER_ID.SQL_COLUMN), 
            result_set.getLong(YoutubeSub.PINGROLE_ID.SQL_COLUMN), 
            result_set.getLong(YoutubeSub.PINGCHANNEL_ID.SQL_COLUMN),
            result_set.getString(YoutubeSub.UPLOADS_PLAYLIST_ID.SQL_COLUMN),
            result_set.getString(YoutubeSub.BROADCASTER_HANDLE.SQL_COLUMN),
            result_set.getString(YoutubeSub.LAST_STREAM_VIDEO_ID.SQL_COLUMN));
    }

    /**
     * @param broadcaster_id
     * @return all YoutubeSub entries with specified broadcaster_id
     * @throws DatabaseException if sql fails for some reason
     */
    public static List<YoutubeSub> pullYoutubeSubsFromBroadcasterId(@NotNull String broadcaster_id) throws DatabaseException {
        // language=sql
        final String sql = """
                SELECT
                    YS.server_id, YS.broadcaster_id, YS.pingrole_id, YS.pingchannel_id,
                    YC.uploads_playlist_id, YC.broadcaster_handle, YC.last_stream_vid_id
                FROM youtube_subscriptions YS JOIN youtube_channels YC ON YS.broadcaster_id = YC.broadcaster_id
                WHERE YS.broadcaster_id = ?
                """;
        Logger.trace("SQL: {}\n?: {}", sql, broadcaster_id);

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setString(1, broadcaster_id);
            ResultSet resultSet = statement.executeQuery();

            List<YoutubeSub> subs = new ArrayList<YoutubeSub>();
            while (resultSet.next()) {
                subs.add(createSubFromResultSet(resultSet));
            }
            Logger.trace("Pulled {} subscriptions with broadcaster_id {}", subs.size(), broadcaster_id);
            return subs;
        } catch (SQLException e) {
            Logger.error(e, "Failed to pull subs with broadcaster id {}.", broadcaster_id);
            throw new DatabaseException("Failed to pull subs from database.");
        }
    }

    /**
     * @param server_id
     * @return all YoutubeSub entries with specified server_id
     * @throws DatabaseException if sql fails for some reason
     */
    public static List<YoutubeSub> pullYoutubeSubsFromServerId(long server_id) throws DatabaseException {
        // language=sql
        final String sql = """
                SELECT
                    YS.server_id, YS.broadcaster_id, YS.pingrole_id, YS.pingchannel_id,
                    YC.uploads_playlist_id, YC.broadcaster_handle, YC.last_stream_vid_id
                FROM youtube_subscriptions YS JOIN youtube_channels YC ON YS.broadcaster_id = YC.broadcaster_id
                WHERE YS.server_id = ?
                """;
        Logger.trace("SQL: {}\n?: {}", sql, server_id);

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, server_id);
            ResultSet resultSet = statement.executeQuery();

            List<YoutubeSub> subs = new ArrayList<YoutubeSub>();
            while (resultSet.next()) {
                subs.add(createSubFromResultSet(resultSet));
            }
            return subs;
        } catch (SQLException e) {
            Logger.error(e, "Failed to pull subs with server id {}.", server_id);
            throw new DatabaseException("Failed to pull subs from database.");
        }
    }

    /**
     * @param server_id
     * @param broadcaster_id
     * @return a YoutubeSub with matching server_id and broadcaster_id; null if not found
     * @throws DatabaseException if connection to database unsuccessful or sql exception
     */
    public static YoutubeSub pullYoutubeSub(long server_id, @NotNull String broadcaster_id) throws DatabaseException {
        // language=sql
        final String sql = """
                SELECT
                    YS.server_id, YS.broadcaster_id, YS.pingrole_id, YS.pingchannel_id,
                    YC.uploads_playlist_id, YC.broadcaster_handle, YC.last_stream_vid_id
                FROM youtube_subscriptions YS JOIN youtube_channels YC ON YS.broadcaster_id = YC.broadcaster_id
                WHERE YS.server_id = ? AND YS.broadcaster_id = ?
                LIMIT 1
                """;
        Logger.trace("SQL: {}\n?: {}, {}", sql, server_id, broadcaster_id);

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, server_id);
            statement.setString(2, broadcaster_id);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return createSubFromResultSet(resultSet);
            } else {
                return null;
            }
        } catch (SQLException e) {
            Logger.error(e, "Failed to pull sub with server_id {} and broadcaster_id {} from {} table.", server_id, broadcaster_id, tableName);
            throw new DatabaseException("Failed to pull sub from database.");
        }
    }

    /**
     * pulls all distinct broadcaster ids that have been subscribed to in the database
     * @throws DatabaseException if sql fails for some reason
     */
    public static List<String> pullSubscriptionBroadcasterIds() throws DatabaseException {
        final String sql = "SELECT DISTINCT broadcaster_id FROM youtube_subscriptions";
        Logger.trace("SQL: {}", sql);

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();

            List<String> subIds = new ArrayList<String>();
            while (resultSet.next()) {
                subIds.add(resultSet.getString(YoutubeSub.BROADCASTER_ID.SQL_COLUMN));
            }
            Logger.trace("Pulled {} distinct broadcaster ids from database. {}", subIds.size());
            return subIds;
        } catch (SQLException e) {
            Logger.error(e, "Failed to pull subscription ids.");
            throw new DatabaseException("Failed to pull subscription ids from database.");
        }
    }

    public static void removeSubscription(long server_id, @NotNull String broadcaster_id) throws DatabaseException {
        final String sql = "DELETE FROM youtube_subscriptions WHERE server_id = ? AND broadcaster_id = ?";
        Logger.trace("SQL: {}\n?: {}", server_id, broadcaster_id);
        
        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, server_id);
            statement.setString(2, broadcaster_id);
            statement.executeUpdate();
            Logger.debug("Removed sub from database with server id {} and broadcaster id {}", server_id, broadcaster_id);
        } catch (SQLException e) {
            Logger.error(e, "Failed to remove subscription with server id {} and broadcaster id {}.", server_id, broadcaster_id);
            throw new DatabaseException("Failed to remove subscription in database.");
        }
    }

    public static void setLastStreamVideoId(@NotNull String broadcaster_id, @NotNull String last_stream_video_id) throws DatabaseException {
        YoutubeChannelsTable.setLastStreamVideoId(broadcaster_id, last_stream_video_id);
    }
}
