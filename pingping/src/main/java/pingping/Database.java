package pingping;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

public class Database {
    protected static final String connectionUrl = "jdbc:sqlite:pingping.db";
    protected static Connection connection = null;

    public static boolean connect() {
        try {
            connection = DriverManager.getConnection(connectionUrl);
            System.out.println("Database connection successful.");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (connection != null) {
                    try {
                        connection.close();
                        System.out.println("SQLite connection closed by shutdown hook.");
                    } catch (SQLException e) {
                        System.err.println("Error closing SQLite connection in shutdown hook: " + e.getMessage());
                    }
                }
            }));

            return true;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return false;
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
                return true;
            } catch (SQLException e) {
                System.err.println(e.getMessage());
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
                return true;
            } catch (SQLException e) {
                System.err.println(e.getMessage());
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
                System.err.println(e.getMessage());
                return null;
            }
        }
    }

    public class ServerTable {
        public static final String tableName = "servers";
        public static enum Columns {
            SERVER_ID("server_id"),
            TWITCH_DATA_TABLE_ID("twitch_data_table_id");

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
        public static boolean insertRow(long server_id) {
            final String sql = "INSERT OR IGNORE INTO " + tableName+"("+Columns.SERVER_ID+") VALUES(?)";
            try (PreparedStatement statement = Database.connection.prepareStatement(sql)) {
                statement.setLong(1, server_id);
                statement.executeUpdate();

                return Database.TwitchTable.insertRow(server_id);
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                return false;
            }
        }
    }

    public static class TwitchTable {
        public static final String tableName = "twitch_data";
        public static enum Columns {
            SERVER_ID("server_id"),
            SUBSCRIPTIONS("subscriptions");

            public final String sqlColumnName;
            private Columns(String sqlColumnName) {
                this.sqlColumnName = sqlColumnName;
            }
            @Override
            public String toString() {
                return this.sqlColumnName;
            }
        }

        private static boolean insertRow(long server_id) {
            final String sql = "INSERT OR IGNORE INTO " + tableName+"("+Columns.SERVER_ID+") VALUES(?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, server_id);
                statement.executeUpdate();
                return true;
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                return false;
            }
        }

        // public static List<String> getSubscribedIds(long server_id) {
            
        // }
    }

    public static void createBaseTables() {
        String globalTable = "CREATE TABLE IF NOT EXISTS " + GlobalTable.tableName + " (" +
            GlobalTable.Columns.BOT_ID + " INTEGER PRIMARY KEY," +
            GlobalTable.Columns.TWITCH_CONDUIT_ID + " TEXT" +
            ");";
        String serverTable = "CREATE TABLE IF NOT EXISTS " + ServerTable.tableName + " (" +
            ServerTable.Columns.SERVER_ID + " INTEGER PRIMARY KEY," +
            ServerTable.Columns.TWITCH_DATA_TABLE_ID + " INTEGER" +
            ");";
        
        try {
            connection.createStatement().execute(globalTable);
            connection.createStatement().execute(serverTable);
            System.out.println("Database tables created.");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    // public static boolean createTwitchDataTable(long server_id) {
    //     String twitchTable = "CREATE TABLE IF NOT EXISTS " + TwitchTable.tableName + " (" +
    //         TwitchTable.Columns.SERVER_ID + " INTEGER PRIMARY KEY," +
    //         TwitchTable.Columns.SUBSCRIPTIONS + " TEXT," + 
    //         "FOREIGN KEY (server_id) REFERENCES servers(server_id) ON DELETE CASCADE" + 
    //         ");";
    // }
}
