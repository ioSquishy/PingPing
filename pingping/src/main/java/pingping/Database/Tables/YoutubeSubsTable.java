package pingping.Database.Tables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Database.OrmObjects.StreamerSubscription;
import pingping.Database.OrmObjects.YoutubeSub;
import pingping.Exceptions.DatabaseException;

public class YoutubeSubsTable {
    public static final String tableName = "youtube_subscriptions";
    // columns in YoutubeSub.java

    public static String tableCreationSql() {
        String sql = "CREATE TABLE IF NOT EXISTS " + YoutubeSubsTable.tableName + " (" +
            StreamerSubscription.SERVER_ID + " INTEGER NOT NULL," +
            StreamerSubscription.BROADCASTER_ID + " STRING NOT NULL," +
            StreamerSubscription.PINGROLE_ID + " INTEGER NOT NULL," +
            StreamerSubscription.PINGCHANNEL_ID + " INTEGER NOT NULL," +
            YoutubeSub.UPLOADS_PLAYLIST_ID + " STRING NOT NULL," +
            YoutubeSub.BROADCASTER_HANDLE + " STRING NOT NULL," +
            YoutubeSub.LAST_STREAM_VIDEO_ID + " STRING," +
            "PRIMARY KEY ("+StreamerSubscription.SERVER_ID+","+StreamerSubscription.BROADCASTER_ID+")," +
            "FOREIGN KEY ("+StreamerSubscription.SERVER_ID+") REFERENCES "+ServerTable.tableName+"("+ServerTable.Columns.SERVER_ID+") ON DELETE CASCADE" + 
            ");";
        Logger.trace("Youtube table create SQL: {}", sql);
        return sql;
    }

    public static void insertSubscription(@NotNull YoutubeSub sub) throws DatabaseException {
        insertSubscription(sub.server_id, sub.broadcaster_id, sub.pingrole_id, sub.pingchannel_id, sub.uploads_playlist_id, sub.broadcaster_handle, sub.last_stream_video_id);
    }

    public static void insertSubscription(long server_id, @NotNull String broadcaster_id, long pingrole_id, long pingchannel_id, @NotNull String uploads_playlist_id, @NotNull String broadcaster_handle, String last_stream_video_id) throws DatabaseException {
        final String sql = "INSERT OR IGNORE INTO " +
            YoutubeSubsTable.tableName+"("+ YoutubeSub.ALL_COLUMNS +")" + 
            " VALUES(?,?,?,?,?,?,?)";
        Logger.trace("SQL: {}\n?: {}, {}, {}, {}, {}, {}, {}", sql, server_id, broadcaster_id, pingrole_id, pingchannel_id, uploads_playlist_id, broadcaster_handle, last_stream_video_id);
        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            Database.ServerTable.insertEntry(server_id);
            statement.setLong(1, server_id);
            statement.setString(2, broadcaster_id);
            statement.setLong(3, pingrole_id);
            statement.setLong(4, pingchannel_id);
            statement.setString(5, uploads_playlist_id);
            statement.setString(6, broadcaster_handle);
            statement.setString(7, last_stream_video_id);
            statement.executeUpdate();
        } catch (SQLException e) {
            Logger.error(e, "Failed to insert subscription with server_id {} and broadcaster_id {} into {} table.", server_id, broadcaster_id, tableName);
            throw new DatabaseException("Failed to insert subscription into database.");
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
        final String sql = "SELECT " + YoutubeSub.ALL_COLUMNS +
            " FROM " + YoutubeSubsTable.tableName +
            " WHERE " + YoutubeSub.BROADCASTER_ID + " = ?";
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
        final String sql = "SELECT " + YoutubeSub.ALL_COLUMNS +
            " FROM " + YoutubeSubsTable.tableName +
            " WHERE " + YoutubeSub.SERVER_ID + " = ?";
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
        final String sql = "SELECT " + YoutubeSub.ALL_COLUMNS +
            " FROM " + YoutubeSubsTable.tableName +
            " WHERE " + YoutubeSub.SERVER_ID + " = ?" +
                " AND " + YoutubeSub.BROADCASTER_ID + " = ?" +
            " LIMIT 1";
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
        final String sql = "SELECT DISTINCT " + YoutubeSub.BROADCASTER_ID +
            " FROM " + YoutubeSubsTable.tableName;
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
        final String sql = "DELETE FROM " + YoutubeSubsTable.tableName + 
            " WHERE " + YoutubeSub.SERVER_ID + " = ?" +
                " AND " + YoutubeSub.BROADCASTER_ID + " = ?";
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
        final String sql = "UPDATE " + YoutubeSubsTable.tableName +
            " SET " + YoutubeSub.LAST_STREAM_VIDEO_ID + " = ?" +
            " WHERE " + YoutubeSub.BROADCASTER_ID + " = ?";
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

    public static void updateSubscription(long server_id, @NotNull String broadcaster_id, long updated_pingrole_id, long updated_pingchannel_id, @NotNull String updated_uploads_playlist_id, @NotNull String updated_broadcaster_handle, String updated_last_stream_video_id) throws DatabaseException {
        final String sql = "UPDATE " + YoutubeSubsTable.tableName + 
            " SET " + YoutubeSub.PINGROLE_ID + " = ? ," +
                YoutubeSub.PINGCHANNEL_ID + " = ?," +
                YoutubeSub.UPLOADS_PLAYLIST_ID + " = ?," + 
                YoutubeSub.BROADCASTER_HANDLE + " = ?," +
                YoutubeSub.LAST_STREAM_VIDEO_ID + " = ?" +
            " WHERE " + YoutubeSub.SERVER_ID + " = ?" +
                " AND " + YoutubeSub.BROADCASTER_ID + " = ?";
        Logger.trace("SQL: {}\n?: {},{},{},{},{},{},{}", sql, updated_pingrole_id, updated_pingchannel_id, updated_uploads_playlist_id, updated_broadcaster_handle, updated_last_stream_video_id, server_id, broadcaster_id);
        
        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, updated_pingrole_id);
            statement.setLong(2, updated_pingchannel_id);
            statement.setString(3, updated_uploads_playlist_id);
            statement.setString(4, updated_broadcaster_handle);
            statement.setString(5, updated_last_stream_video_id);
            statement.setLong(6, server_id);
            statement.setString(7, broadcaster_id);
            statement.executeUpdate();
            Logger.debug("Updated sub in database with server id {} and broadcaster id {}", server_id, broadcaster_id);
        } catch (SQLException e) {
            Logger.error(e, "Failed to update subscription with server id {} and broadcaster id {}.", server_id, broadcaster_id);
            throw new DatabaseException("Failed to update subscription in database.");
        }
    }

    public static void updateSubscription(@NotNull YoutubeSub updated_youtube_sub) throws DatabaseException {
        updateSubscription(updated_youtube_sub.server_id, updated_youtube_sub.broadcaster_id, updated_youtube_sub.pingrole_id, updated_youtube_sub.pingchannel_id, updated_youtube_sub.uploads_playlist_id, updated_youtube_sub.broadcaster_handle, updated_youtube_sub.last_stream_video_id);
    }
}
