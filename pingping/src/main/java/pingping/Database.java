package pingping;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    public static final String connectionUrl = "jdbc:sqlite:pingping.db";
    private static Connection connection = null;

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
        public static enum Columns {
            BOT_ID("bot_id"),
            TWITCH_CONDUIT_ID("twitch_conduit_id");

            public final String sqlColumnName;
            private Columns(String sqlColumnName) {
                this.sqlColumnName = sqlColumnName;
            }
            @Override
            public String toString() {
                return this.sqlColumnName;
            }
        }
    }

    public static class ServerTable {
        public static enum Columns {
            SERVER_ID("server_id");

            public final String sqlColumnName;
            private Columns(String sqlColumnName) {
                this.sqlColumnName = sqlColumnName;
            }
            @Override
            public String toString() {
                return this.sqlColumnName;
            }
        }
    }

    public static class TwitchTable {
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
    }

    public static void createTables() {
        String globalTable = "CREATE TABLE IF NOT EXISTS global (" +
            GlobalTable.Columns.BOT_ID + " INTEGER PRIMARY KEY," +
            GlobalTable.Columns.TWITCH_CONDUIT_ID + " INTEGER" +
            ");";
        String serverTable = "CREATE TABLE IF NOT EXISTS servers (" +
            ServerTable.Columns.SERVER_ID + " INTEGER PRIMARY KEY" + 
            ");";
        String twitchTable = "CREATE TABLE IF NOT EXISTS twitch_data (" +
            TwitchTable.Columns.SERVER_ID + " INTEGER PRIMARY KEY," +
            TwitchTable.Columns.SUBSCRIPTIONS + " TEXT," + 
            "FOREIGN KEY (server_id) REFERENCES servers(server_id) ON DELETE CASCADE" + 
            ");";
        try {
            connection.createStatement().execute(globalTable);
            connection.createStatement().execute(serverTable);
            connection.createStatement().execute(twitchTable);
            System.out.println("Database tables created.");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }
}
