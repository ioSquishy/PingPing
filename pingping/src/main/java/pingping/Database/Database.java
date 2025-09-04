package pingping.Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import pingping.Database.OrmObjects.TwitchSub;

public class Database {
    protected static final String connectionUrl = "jdbc:sqlite:notpingping.db";
    protected static Connection connection = null;

    public static void connect() throws SQLException {
        connection = DriverManager.getConnection(connectionUrl);
        connection.createStatement().execute("PRAGMA foreign_keys = ON;");
        Logger.info("Database connection successful.");
        Database.createBaseTables();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (connection != null) {
                try {
                    connection.close();
                    Logger.info("SQLite connection closed by shutdown hook.");
                } catch (SQLException e) {
                    Logger.error(e, "Error closing SQLite connection in shutdown hook");
                }
            }
        }));
    }

    public static class GlobalTable {
        public static final String tableName = "global";
        public static enum Columns {
            BOT_ID("bot_id"),
            TWITCH_CONDUIT_ID("twitch_conduit_id"); // max of 5 conduits per twitch user account

            public final String sqlColumnName;
            private Columns(String sqlColumnName) {
                this.sqlColumnName = sqlColumnName;
            }
            @Override
            public String toString() {
                return this.sqlColumnName;
            }
        }

        public static boolean insertRow(long bot_id) {
            final String sql = "INSERT OR IGNORE INTO " + tableName+"("+Columns.BOT_ID+") VALUES(?)";
            try (PreparedStatement statement = Database.connection.prepareStatement(sql)) {
                statement.setLong(1, bot_id);
                statement.executeUpdate();
                Logger.debug("Inserted bot id {} into {}", bot_id, tableName);
                return true;
            } catch (SQLException e) {
                Logger.error("Failed to insert bot id {} into {}", bot_id, tableName);
                Logger.error(e);
                return false;
            }
        }

        public static boolean setConduitId(long bot_id, String conduit_id) {
            final String sql = "UPDATE " + tableName +
                " SET " + Columns.TWITCH_CONDUIT_ID + " = ?" + 
                " WHERE " + Columns.BOT_ID + " = ?";
            
            try (PreparedStatement statement = Database.connection.prepareStatement(sql)) {
                statement.setString(1, conduit_id);
                statement.setLong(2, bot_id);
                statement.executeUpdate();
                Logger.debug("Set conduit id for bot id {} to {}", bot_id, conduit_id);
                return true;
            } catch (SQLException e) {
                Logger.error("Failed to set conduit id for bot id {} to {}", bot_id, conduit_id);
                Logger.error(e);
                return false;
            }
        }

        public static @Nullable String getConduitId(long bot_id) {
            final String sql = "SELECT " + Columns.TWITCH_CONDUIT_ID + 
                " FROM " + tableName + 
                " WHERE " + Columns.BOT_ID + " = ? " +
                " LIMIT 1";
            
            try (PreparedStatement statement = Database.connection.prepareStatement(sql)) {
                statement.setLong(1, bot_id);
                ResultSet result = statement.executeQuery();
                if (!result.next()) {
                    Logger.debug("getConduitId() did not retrieve any results");
                }
                return result.getString(Columns.TWITCH_CONDUIT_ID.sqlColumnName);
            } catch (SQLException e) {
                Logger.error("Failed to retrieve conduit id for bot id {}", bot_id);
                Logger.error(e);
                return null;
            }
        }
    }

    public class ServerTable {
        public static final String tableName = "servers";
        public static enum Columns {
            SERVER_ID("server_id"); // INTEGER

            public final String sqlColumnName;
            private Columns(String sqlColumnName) {
                this.sqlColumnName = sqlColumnName;
            }
            @Override
            public String toString() {
                return this.sqlColumnName;
            }
        }

        /**
         * will also create a new TwitchTable table with the specified server_id
         * @param server_id
         * @return
         */
        public static boolean insertEntry(long server_id) {
            final String sql = "INSERT OR IGNORE INTO " + tableName+"("+Columns.SERVER_ID+") VALUES(?)";
            try (PreparedStatement statement = Database.connection.prepareStatement(sql)) {
                statement.setLong(1, server_id);
                statement.executeUpdate();
                return true;
            } catch (SQLException e) {
                Logger.error(e);
                return false;
            }
        }

        public static boolean removeEntry(long server_id) {
            final String sql = "DELETE FROM " + ServerTable.tableName + " WHERE " + Columns.SERVER_ID + " = ?";
            try (PreparedStatement statement = Database.connection.prepareStatement(sql)) {
                statement.setLong(1, server_id);
                statement.executeUpdate();
                return true;
            } catch (SQLException e) {
                Logger.error(e);
                return false;
            }
        }
    }

    public static class TwitchSubsTable {
        public static final String tableName = "twitch_subscriptions";
        

        public static boolean insertSubscription(TwitchSub sub) {
            return insertSubscription(sub.server_id, sub.broadcaster_id, sub.eventsub_id, sub.pingrole_id, sub.pingchannel_id);
        }

        public static boolean insertSubscription(long server_id, long broadcaster_id, String eventsub_id, long pingrole_id, long pingchannel_id) {
            final String sql = "INSERT OR IGNORE INTO " +
                tableName+"("+TwitchSub.Columns.SERVER_ID+","+TwitchSub.Columns.BROADCASTER_ID+","+TwitchSub.Columns.EVENTSUB_ID+","+TwitchSub.Columns.PINGROLE_ID+","+TwitchSub.Columns.PINGCHANNEL_ID+")" + 
                " VALUES(?,?,?,?,?)";
            
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                if (!Database.ServerTable.insertEntry(server_id)) {
                    return false;
                }
                statement.setLong(1, server_id);
                statement.setLong(2, broadcaster_id);
                statement.setString(3, eventsub_id);
                statement.setLong(4, pingrole_id);
                statement.setLong(5, pingchannel_id);
                statement.executeUpdate();
                return true;
            } catch (SQLException e) {
                Logger.error(e, "Failed to insert subscription into database.");
                return false;
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
                result_set.getLong(TwitchSub.Columns.SERVER_ID.sql_column_name), 
                result_set.getLong(TwitchSub.Columns.BROADCASTER_ID.sql_column_name), 
                result_set.getString(TwitchSub.Columns.EVENTSUB_ID.sql_column_name),
                result_set.getLong(TwitchSub.Columns.PINGROLE_ID.sql_column_name), 
                result_set.getLong(TwitchSub.Columns.PINGCHANNEL_ID.sql_column_name));
        }

        public static List<TwitchSub> pullTwitchSubs(long server_id) {
            final String sql = "SELECT " + TwitchSub.Columns.SERVER_ID+","+TwitchSub.Columns.BROADCASTER_ID+","+TwitchSub.Columns.EVENTSUB_ID+","+TwitchSub.Columns.PINGROLE_ID+","+TwitchSub.Columns.PINGCHANNEL_ID +
                " FROM " + TwitchSubsTable.tableName +
                " WHERE " + TwitchSub.Columns.SERVER_ID + " = ?";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, server_id);
                ResultSet resultSet = statement.executeQuery();

                List<TwitchSub> subs = new ArrayList<TwitchSub>();
                while (resultSet.next()) {
                    subs.add(createSubFromResultSet(resultSet));
                }
                return subs;
            } catch (SQLException e) {
                Logger.error(e);
                return Collections.emptyList();
            }
        }

        /**
         * @param server_id
         * @param broadcaster_id
         * @return a twitch sub with matching server_id and broadcaster_id; null if not found
         */
        public static TwitchSub pullTwitchSub(long server_id, long broadcaster_id) {
            final String sql = "SELECT " + TwitchSub.Columns.SERVER_ID+","+TwitchSub.Columns.BROADCASTER_ID+","+TwitchSub.Columns.PINGROLE_ID+","+TwitchSub.Columns.PINGCHANNEL_ID +
                " FROM " + TwitchSubsTable.tableName +
                " WHERE " + TwitchSub.Columns.SERVER_ID + " = ?" +
                " AND " + TwitchSub.Columns.BROADCASTER_ID + " = ?" +
                " LIMIT 1";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, server_id);
                statement.setLong(2, broadcaster_id);
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    return createSubFromResultSet(resultSet);
                } else {
                    return null;
                }
            } catch (SQLException e) {
                Logger.error(e);
                return null;
            }
        }

        public static List<String> pullSubscriptionIds(long server_id) {
            final String sql = "SELECT " + TwitchSub.Columns.BROADCASTER_ID +
                " FROM " + TwitchSubsTable.tableName +
                " WHERE " + TwitchSub.Columns.SERVER_ID + " = ?";
            
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, server_id);
                ResultSet resultSet = statement.executeQuery();

                List<String> subIds = new ArrayList<String>();
                while (resultSet.next()) {
                    subIds.add(resultSet.getString(TwitchSub.Columns.BROADCASTER_ID.sql_column_name));
                }
                return subIds;
            } catch (SQLException e) {
                Logger.error(e);
                return Collections.emptyList();
            }
        }

        public static boolean removeSubscription(long server_id, long broadcaster_id) {
            // TODO
            return false;
        }

        public static boolean updateSubscription(long server_id, long broadcaster_id, long updated_pingrole_id, long updated_pingchannel_id) {
            //TODO
            return false;
        }
    }

    public static void createBaseTables() {
        String globalTable = "CREATE TABLE IF NOT EXISTS " + GlobalTable.tableName + " (" +
            GlobalTable.Columns.BOT_ID + " INTEGER PRIMARY KEY," +
            GlobalTable.Columns.TWITCH_CONDUIT_ID + " TEXT" +
            ");";
        String serverTable = "CREATE TABLE IF NOT EXISTS " + ServerTable.tableName + " (" +
            ServerTable.Columns.SERVER_ID + " INTEGER PRIMARY KEY" +
            ");";
        String twitchTable = "CREATE TABLE IF NOT EXISTS " + TwitchSubsTable.tableName + " (" +
            TwitchSub.Columns.SERVER_ID + " INTEGER NOT NULL," +
            TwitchSub.Columns.BROADCASTER_ID + " INTEGER NOT NULL," +
            TwitchSub.Columns.EVENTSUB_ID + " STRING NOT NULL," +
            TwitchSub.Columns.PINGROLE_ID + " INTEGER NOT NULL," +
            TwitchSub.Columns.PINGCHANNEL_ID + " INTEGER NOT NULL," +
            "PRIMARY KEY ("+TwitchSub.Columns.SERVER_ID+","+TwitchSub.Columns.BROADCASTER_ID+")," +
            "FOREIGN KEY ("+TwitchSub.Columns.SERVER_ID+") REFERENCES "+ServerTable.tableName+"("+ServerTable.Columns.SERVER_ID+") ON DELETE CASCADE" + 
            ");";
        try {
            connection.createStatement().execute(globalTable);
            Logger.trace("Created globalTable");
            connection.createStatement().execute(serverTable);
            Logger.trace("Created serverTable");
            connection.createStatement().execute(twitchTable);
            Logger.trace("Created twitchTable");
            Logger.info("Database base tables instantiated.");
        } catch (SQLException e) {
            Logger.error(e);
        }
    }
}
