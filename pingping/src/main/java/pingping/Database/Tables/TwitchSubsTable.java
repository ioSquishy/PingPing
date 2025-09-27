package pingping.Database.Tables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Database.OrmObjects.TwitchSub;
import pingping.Exceptions.DatabaseException;

public class TwitchSubsTable {
    public static final String tableName = "twitch_subscriptions";

    public static void insertSubscription(@NotNull TwitchSub sub) throws DatabaseException {
        insertSubscription(sub.server_id, sub.broadcaster_id, sub.eventsub_id, sub.pingrole_id, sub.pingchannel_id);
    }

    public static void insertSubscription(long server_id, long broadcaster_id, @NotNull String eventsub_id, long pingrole_id, long pingchannel_id) throws DatabaseException {
        final String sql = "INSERT OR IGNORE INTO " +
            TwitchSubsTable.tableName+"("+TwitchSub.SERVER_ID+","+TwitchSub.BROADCASTER_ID+","+TwitchSub.EVENTSUB_ID+","+TwitchSub.PINGROLE_ID+","+TwitchSub.PINGCHANNEL_ID+")" + 
            " VALUES(?,?,?,?,?)";
        Logger.trace("SQL: {}\n?: {}, {}, {}, {}, {}", sql, server_id, broadcaster_id, eventsub_id, pingrole_id, pingchannel_id);
        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            Database.ServerTable.insertEntry(server_id);
            statement.setLong(1, server_id);
            statement.setLong(2, broadcaster_id);
            statement.setString(3, eventsub_id);
            statement.setLong(4, pingrole_id);
            statement.setLong(5, pingchannel_id);
            statement.executeUpdate();
        } catch (SQLException e) {
            Logger.error(e, "Failed to insert subscription with server_id {} and broadcaster_id {} into {} table.", server_id, broadcaster_id, tableName);
            throw new DatabaseException("Failed to insert subscription into database.");
        }
    }

    /**
     * returns TwitchSub object from current iteration of resultSet
     * does not increment ResultSet index
     * @param result_set assumes the following columns are present: Columns.SERVER_ID+","+Columns.BROADCASTER_ID+","+Columns.EVENTSUB_ID+","+Columns.PINGROLE_ID+","+Columns.PINGCHANNEL_ID
     * @return
     * @throws SQLException
     */
    private static TwitchSub createSubFromResultSet(ResultSet result_set) throws SQLException {
        return new TwitchSub(
            result_set.getLong(TwitchSub.SERVER_ID.SQL_COLUMN), 
            result_set.getLong(TwitchSub.BROADCASTER_ID.SQL_COLUMN), 
            result_set.getLong(TwitchSub.PINGROLE_ID.SQL_COLUMN), 
            result_set.getLong(TwitchSub.PINGCHANNEL_ID.SQL_COLUMN),
            result_set.getString(TwitchSub.EVENTSUB_ID.SQL_COLUMN));
    }

    public static List<TwitchSub> pullTwitchSubsFromBroadcasterId(long broadcaster_id) throws DatabaseException {
        final String sql = "SELECT " + TwitchSub.SERVER_ID+","+TwitchSub.BROADCASTER_ID+","+TwitchSub.EVENTSUB_ID+","+TwitchSub.PINGROLE_ID+","+TwitchSub.PINGCHANNEL_ID +
            " FROM " + TwitchSubsTable.tableName +
            " WHERE " + TwitchSub.BROADCASTER_ID + " = ?";
        Logger.trace("SQL: {}\n?: {}", sql, broadcaster_id);

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, broadcaster_id);
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

    public static List<TwitchSub> pullTwitchSubsFromServerId(long server_id) throws DatabaseException {
        final String sql = "SELECT " + TwitchSub.SERVER_ID+","+TwitchSub.BROADCASTER_ID+","+TwitchSub.EVENTSUB_ID+","+TwitchSub.PINGROLE_ID+","+TwitchSub.PINGCHANNEL_ID +
            " FROM " + TwitchSubsTable.tableName +
            " WHERE " + TwitchSub.SERVER_ID + " = ?";
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
    public static TwitchSub pullTwitchSub(long server_id, long broadcaster_id) throws DatabaseException {
        final String sql = "SELECT " + TwitchSub.SERVER_ID+","+TwitchSub.BROADCASTER_ID+","+TwitchSub.EVENTSUB_ID+","+TwitchSub.PINGROLE_ID+","+TwitchSub.PINGCHANNEL_ID +
            " FROM " + TwitchSubsTable.tableName +
            " WHERE " + TwitchSub.SERVER_ID + " = ?" +
                " AND " + TwitchSub.BROADCASTER_ID + " = ?" +
            " LIMIT 1";
        Logger.trace("SQL: {}\n?: {}, {}", sql, server_id, broadcaster_id);

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, server_id);
            statement.setLong(2, broadcaster_id);
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
    public static List<Long> pullSubscriptionBroadcasterIds() throws DatabaseException {
        final String sql = "SELECT DISTINCT " + TwitchSub.BROADCASTER_ID +
            " FROM " + TwitchSubsTable.tableName;
        Logger.trace("SQL: {}", sql);

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();

            List<Long> subIds = new ArrayList<Long>();
            while (resultSet.next()) {
                subIds.add(resultSet.getLong(TwitchSub.BROADCASTER_ID.SQL_COLUMN));
            }
            Logger.trace("Pulled {} distinct broadcaster ids from database. {}", subIds.size());
            return subIds;
        } catch (SQLException e) {
            Logger.error(e, "Failed to pull subscription ids.");
            throw new DatabaseException("Failed to pull subscription ids from database.");
        }
    }

    public static List<String> pullSubscriptionBroadcasterIds(long server_id) throws DatabaseException {
        final String sql = "SELECT " + TwitchSub.BROADCASTER_ID +
            " FROM " + TwitchSubsTable.tableName +
            " WHERE " + TwitchSub.SERVER_ID + " = ?";
        Logger.trace("SQL: {}\n?: {}", sql, server_id);
        
        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, server_id);
            ResultSet resultSet = statement.executeQuery();

            List<String> subIds = new ArrayList<String>();
            while (resultSet.next()) {
                subIds.add(resultSet.getString(TwitchSub.BROADCASTER_ID.SQL_COLUMN));
            }
            return subIds;
        } catch (SQLException e) {
            Logger.error(e, "Failed to pull subscription ids for server id: {}.", server_id);
            throw new DatabaseException("Failed to pull subscription ids from database.");
        }
    }

    public static void removeSubscription(long server_id, long broadcaster_id) throws DatabaseException {
        final String sql = "DELETE FROM " + TwitchSubsTable.tableName + 
            " WHERE " + TwitchSub.SERVER_ID + " = ?" +
                " AND " + TwitchSub.BROADCASTER_ID + " = ?";
        Logger.trace("SQL: {}\n?: {}", server_id, broadcaster_id);
        
        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, server_id);
            statement.setLong(2, broadcaster_id);
            statement.executeUpdate();
            Logger.debug("Removed twitch sub from database with server id {} and broadcaster id {}", server_id, broadcaster_id);
        } catch (SQLException e) {
            Logger.error(e, "Failed to remove subscription with server id {} and broadcaster id {}.", server_id, broadcaster_id);
            throw new DatabaseException("Failed to remove subscription in database.");
        }
    }

    public static void setEventSubId(long broadcaster_id, @NotNull String event_sub_id) throws DatabaseException {
        final String sql = "UPDATE " + TwitchSubsTable.tableName +
            " SET " + TwitchSub.EVENTSUB_ID + " = ?" +
            " WHERE " + TwitchSub.BROADCASTER_ID + " = ?";
        Logger.trace("SQL: {}\n?: {}", event_sub_id, broadcaster_id);

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setString(1, event_sub_id);
            statement.setLong(2, broadcaster_id);
            statement.executeUpdate();
            Logger.debug("Updated eventSubIds in database for broadcasters with id {} to {}", broadcaster_id, event_sub_id);
        } catch (SQLException e) {
            Logger.error(e, "Failed to update eventSubIds in database for broadcasters with id {} to {}", broadcaster_id, event_sub_id);
            throw new DatabaseException("Failed to update eventSubIds in database.");
        }
    }

    public static void updateSubscription(long server_id, long broadcaster_id, @NotNull String updated_eventsub_id, long updated_pingrole_id, long updated_pingchannel_id) throws DatabaseException {
        final String sql = "UPDATE " + TwitchSubsTable.tableName + 
            " SET " + TwitchSub.EVENTSUB_ID + " = ? ," + 
                TwitchSub.PINGROLE_ID + " = ? ," +
                TwitchSub.PINGCHANNEL_ID + " = ?" +
            " WHERE " + TwitchSub.SERVER_ID + " = ?" +
                " AND " + TwitchSub.BROADCASTER_ID + " = ?";
        Logger.trace("SQL: {}\n?: {},{},{},{},{}", sql, updated_eventsub_id, updated_pingrole_id, updated_pingchannel_id, server_id, broadcaster_id);
        
        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setString(1, updated_eventsub_id);
            statement.setLong(2, updated_pingrole_id);
            statement.setLong(3, updated_pingchannel_id);
            statement.setLong(4, server_id);
            statement.setLong(5, broadcaster_id);
            statement.executeUpdate();
            Logger.debug("Updated twitch sub in database with server id {} and broadcaster id {}", server_id, broadcaster_id);
        } catch (SQLException e) {
            Logger.error(e, "Failed to update subscription with server id {} and broadcaster id {}.", server_id, broadcaster_id);
            throw new DatabaseException("Failed to update subscription in database.");
        }
    }

    public static void updateSubscription(@NotNull TwitchSub updated_twitch_sub) throws DatabaseException {
        updateSubscription(updated_twitch_sub.server_id, updated_twitch_sub.broadcaster_id, updated_twitch_sub.eventsub_id, updated_twitch_sub.pingrole_id, updated_twitch_sub.pingchannel_id);
    }
}
