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
import pingping.Database.OrmObjects.TwitchSub;
import pingping.Exceptions.DatabaseException;

public class TwitchSubsTable {
    public static final String tableName = "twitch_subscriptions";
    // columns in TwitchSub.java

    public static String tableCreationSql() {
        // language=sql
        final String sql = MessageFormat.format("""
                CREATE TABLE IF NOT EXISTS {0} (
                    {1} INTEGER UNIQUE NOT NULL, -- SERVER_ID
                    {2} TEXT NOT NULL, -- BROADCASTER_ID
                    {3} INTEGER NOT NULL, -- PINGROLE_ID
                    {4} INTEGER NOT NULL, -- PINGCHANNEL_ID
                    PRIMARY KEY ({1}, {2}),
                    FOREIGN KEY ({1}) REFERENCES {5}({6}) ON DELETE CASCADE,
                    FOREIGN KEY ({2}) REFERENCES {7}({2}) ON DELETE CASCADE
                )
                """, TwitchSubsTable.tableName, 
                    TwitchSub.SERVER_ID, TwitchSub.BROADCASTER_ID, TwitchSub.PINGROLE_ID, 
                    TwitchSub.PINGCHANNEL_ID, ServerTable.tableName, ServerTable.Columns.SERVER_ID,
                    TwitchChannelsTable.tableName);
        Logger.trace("Twitch table create SQL: {}", sql);
        return sql;
    }

    public static void insertSubscription(@NotNull TwitchSub sub) throws DatabaseException {
        insertSubscription(sub.server_id, sub.broadcaster_id, sub.pingrole_id, sub.pingchannel_id, sub.eventsub_id);
    }

    public static void insertSubscription(long server_id, @NotNull String broadcaster_id, long pingrole_id, long pingchannel_id, @NotNull String eventsub_id) throws DatabaseException {
        // language=sql
        final String sql = """
                INSERT OR IGNORE INTO twitch_subscriptions (
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
            TwitchChannelsTable.insertEventSubId(broadcaster_id, eventsub_id);
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
     * returns TwitchSub object from current iteration of resultSet
     * does not increment ResultSet index
     * @param result_set assumes the following columns are present (not order-dependent): Columns.SERVER_ID+","+Columns.BROADCASTER_ID+","+Columns.PINGROLE_ID+","+Columns.PINGCHANNEL_ID+","+Columns.EVENTSUB_ID
     * @throws SQLException
     */
    private static TwitchSub createSubFromResultSet(ResultSet result_set) throws SQLException {
        return new TwitchSub(
            result_set.getLong(TwitchSub.SERVER_ID.SQL_COLUMN), 
            result_set.getString(TwitchSub.BROADCASTER_ID.SQL_COLUMN), 
            result_set.getLong(TwitchSub.PINGROLE_ID.SQL_COLUMN), 
            result_set.getLong(TwitchSub.PINGCHANNEL_ID.SQL_COLUMN),
            result_set.getString(TwitchSub.EVENTSUB_ID.SQL_COLUMN));
    }

    /**
     * @param broadcaster_id
     * @return all TwitchSub entries with specified broadcaster_id
     * @throws DatabaseException if sql fails for some reason
     */
    public static List<TwitchSub> pullTwitchSubsFromBroadcasterId(@NotNull String broadcaster_id) throws DatabaseException {
        // language=sql
        final String sql = """
                SELECT
                    TS.server_id,
                    TS.broadcaster_id,
                    TS.pingrole_id,
                    TS.pingchannel_id,
                    TC.eventsub_id
                FROM twitch_subscriptions TS JOIN twitch_channels TC ON (TS.broadcaster_id = TC.broadcaster_id)
                WHERE TS.broadcaster_id = ?
                """;
        Logger.trace("SQL: {}\n?: {}", sql, broadcaster_id);

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setString(1, broadcaster_id);
            ResultSet resultSet = statement.executeQuery();

            List<TwitchSub> subs = new ArrayList<TwitchSub>();
            while (resultSet.next()) {
                subs.add(createSubFromResultSet(resultSet));
            }
            Logger.trace("Pulled {} subscriptions with broadcaster_id {}", subs.size(), broadcaster_id);
            return subs;
        } catch (SQLException e) {
            Logger.error(e, "Failed to pull twitch subs with broadcaster id {}.", broadcaster_id);
            throw new DatabaseException("Failed to pull twitch subs from database.");
        }
    }

    /**
     * Gets the count of servers with subscriptions to a broadcaster
     * @param broadcaster_id
     * @return
     * @throws DatabaseException
     */
    public static int getNumSubsForBroadcasterId(@NotNull String broadcaster_id) throws DatabaseException {
        final String sql = "SELECT COUNT(server_id) FROM twitch_subscriptions WHERE broadcaster_id = ?";
        Logger.trace("SQL: {}\n?: {}", sql, broadcaster_id);

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setString(1, broadcaster_id);
            ResultSet resultSet = statement.executeQuery();
            int numSubs = resultSet.getInt(1); 

            Logger.trace("{} number of subs for broadcaster_id {}", numSubs, broadcaster_id);
            return resultSet.getInt(1);
        } catch (SQLException e) {
            Logger.error(e, "Failed to pull twitch subs with broadcaster id {}.", broadcaster_id);
            throw new DatabaseException("Failed to pull twitch subs from database.");
        }
    }

    /**
     * @param server_id
     * @return all TwitchSub entries with specified server_id
     * @throws DatabaseException if sql fails for some reason
     */
    public static List<TwitchSub> pullTwitchSubsFromServerId(long server_id) throws DatabaseException {
        // language=sql
        final String sql = """
                SELECT
                    TS.server_id,
                    TS.broadcaster_id,
                    TS.pingrole_id,
                    TS.pingchannel_id,
                    TC.eventsub_id
                FROM twitch_subscriptions TS JOIN twitch_channels TC ON (TS.broadcaster_id = TC.broadcaster_id)
                WHERE TS.server_id = ?
                """;
        Logger.trace("SQL: {}\n?: {}", sql, server_id);

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, server_id);
            ResultSet resultSet = statement.executeQuery();

            List<TwitchSub> subs = new ArrayList<TwitchSub>();
            while (resultSet.next()) {
                subs.add(createSubFromResultSet(resultSet));
            }
            return subs;
        } catch (SQLException e) {
            Logger.error(e, "Failed to pull twitch subs with server id {}.", server_id);
            throw new DatabaseException("Failed to pull twitch subs from database.");
        }
    }

    /**
     * @param server_id
     * @param broadcaster_id
     * @return a twitch sub with matching server_id and broadcaster_id; null if not found
     * @throws DatabaseException if connection to database unsuccessful or sql exception
     */
    public static TwitchSub pullTwitchSub(long server_id, @NotNull String broadcaster_id) throws DatabaseException {
        // language=sql
        final String sql = """
                SELECT
                    TS.server_id,
                    TS.broadcaster_id,
                    TS.pingrole_id,
                    TS.pingchannel_id,
                    TC.eventsub_id
                FROM twitch_subscriptions TS JOIN twitch_channels TC ON (TS.broadcaster_id = TC.broadcaster_id)
                WHERE TS.server_id = ? AND TS.broadcaster_id = ?
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
            Logger.error(e, "Failed to pull twitch sub with server_id {} and broadcaster_id {} from {} table.", server_id, broadcaster_id, tableName);
            throw new DatabaseException("Failed to pull twitch sub from database.");
        }
    }

    /**
     * pulls all distinct broadcaster ids that have been subscribed to in the database
     * @throws DatabaseException if sql fails for some reason
     */
    public static List<String> pullSubscriptionBroadcasterIds() throws DatabaseException {
        final String sql = "SELECT DISTINCT broadcaster_id FROM twitch_subscriptions";
        Logger.trace("SQL: {}", sql);

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();

            List<String> subIds = new ArrayList<String>();
            while (resultSet.next()) {
                subIds.add(resultSet.getString(TwitchSub.BROADCASTER_ID.SQL_COLUMN));
            }
            Logger.trace("Pulled {} distinct broadcaster ids from database. {}", subIds.size());
            return subIds;
        } catch (SQLException e) {
            Logger.error(e, "Failed to pull subscription ids.");
            throw new DatabaseException("Failed to pull subscription ids from database.");
        }
    }

    public static void removeSubscription(long server_id, @NotNull String broadcaster_id) throws DatabaseException {
        final String sql = "DELETE FROM twitch_subscriptions WHERE server_id = ? AND broadcaster_id = ?";
        Logger.trace("SQL: {}\n?: {}", server_id, broadcaster_id);
        
        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, server_id);
            statement.setString(2, broadcaster_id);
            statement.executeUpdate();
            Logger.debug("Removed twitch sub from database with server id {} and broadcaster id {}", server_id, broadcaster_id);
        } catch (SQLException e) {
            Logger.error(e, "Failed to remove subscription with server id {} and broadcaster id {}.", server_id, broadcaster_id);
            throw new DatabaseException("Failed to remove subscription in database.");
        }
    }

    public static void setEventSubId(@NotNull String broadcaster_id, @NotNull String event_sub_id) throws DatabaseException {
        TwitchChannelsTable.setEventSubId(broadcaster_id, event_sub_id);
    }

    public static void updateSubscription(long server_id, @NotNull String broadcaster_id, long updated_pingrole_id, long updated_pingchannel_id, @NotNull String updated_eventsub_id) throws DatabaseException {
        // language=sql
        final String sql = """
                UPDATE twitch_subscriptions
                SET pingrole_id = ?, pingchannel_id = ?
                WHERE server_id = ? AND broadcaster_id = ?
                """;
        Logger.trace("SQL: {}\n?: {},{},{},{}", sql, updated_pingrole_id, updated_pingchannel_id, server_id, broadcaster_id);
        
        Connection databaseConnection = Database.getConnection();
        try (PreparedStatement statement = databaseConnection.prepareStatement(sql)) {
            statement.setLong(1, updated_pingrole_id);
            statement.setLong(2, updated_pingchannel_id);
            statement.setLong(3, server_id);
            statement.setString(4, broadcaster_id);

            databaseConnection.setAutoCommit(false);
            statement.executeUpdate();
            TwitchChannelsTable.setEventSubId(broadcaster_id, updated_eventsub_id);
            databaseConnection.commit();
            Logger.debug("Updated twitch sub in database with server id {} and broadcaster id {}", server_id, broadcaster_id);
        } catch (SQLException e) {
            Logger.error(e, "Failed to update subscription with server id {} and broadcaster id {}.", server_id, broadcaster_id);
            try {
                databaseConnection.rollback();
            } catch (SQLException e1) {
                Logger.error(e1, "Failed to rollback database commit.");
            }
            throw new DatabaseException("Failed to update subscription in database.");
        } finally {
            try {
                Database.getConnection().setAutoCommit(true);
            } catch (SQLException e) {
                Logger.error(e, "Failed to set database auto commit to true.");
                throw new DatabaseException("Failed to set database auto commit to true.");
            }
        }
    }

    public static void updateSubscription(@NotNull TwitchSub updated_twitch_sub) throws DatabaseException {
        updateSubscription(updated_twitch_sub.server_id, updated_twitch_sub.broadcaster_id, updated_twitch_sub.pingrole_id, updated_twitch_sub.pingchannel_id, updated_twitch_sub.eventsub_id);
    }
}
