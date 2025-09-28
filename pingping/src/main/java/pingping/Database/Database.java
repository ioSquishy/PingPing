package pingping.Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.tinylog.Logger;

import pingping.Exceptions.DatabaseException;

public class Database {
    private static final String connectionUrl = "jdbc:sqlite:notpingping.db";
    private Connection connection = null;
    private static Database singleton = null;
    private Database() {}

    public static class GlobalTable extends pingping.Database.Tables.GlobalTable {}
    public static class ServerTable extends pingping.Database.Tables.ServerTable {}
    public static class TwitchSubsTable extends pingping.Database.Tables.TwitchSubsTable {}
    public static class YoutubeSubsTable extends pingping.Database.Tables.YoutubeSubsTable {}

    private static void createBaseTables() throws SQLException {
        Logger.trace("Creating base tables for database.");
        String globalTable = GlobalTable.tableCreationSql();
        Logger.trace("Global table SQL: {}", globalTable);

        String serverTable = ServerTable.tableCreationSql();
        Logger.trace("Server table SQL: {}", serverTable);

        String twitchTable = TwitchSubsTable.tableCreationSql();
        Logger.trace("Twitch table SQL: {}", twitchTable);

        String youtubeTable = YoutubeSubsTable.tableCreationSql();
        Logger.trace("Youtube table SQL: {}", youtubeTable);

        try {
            Database.singleton.connection.createStatement().execute(globalTable);
            Logger.trace("Created globalTable");
            Database.singleton.connection.createStatement().execute(serverTable);
            Logger.trace("Created serverTable");
            Database.singleton.connection.createStatement().execute(twitchTable);
            Logger.trace("Created twitchTable");
            Database.singleton.connection.createStatement().execute(youtubeTable);
            Logger.trace("Created youtubeTable");

            Logger.debug("Database base tables instantiated.");
        } catch (SQLException e) {
            Logger.error(e, "Failed to instantiate base tables.");
        }
    }

    public void connect() throws SQLException {
        connection = DriverManager.getConnection(connectionUrl);
        connection.createStatement().execute("PRAGMA foreign_keys = ON;");
        Database.createBaseTables();
        Logger.info("Database connection successful.");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (connection != null) {
                try {
                    connection.close();
                    System.out.println("SQLite connection closed by shutdown hook.");
                } catch (SQLException e) {
                    System.err.println("Error closing SQLite connection in shutdown hook.");
                    e.printStackTrace();
                }
            }
        }));
    }

    /**
     * 
     * @return database connection
     * @throws DatabaseException if failed to connect to database
     */
    public static Connection getConnection() throws DatabaseException {
        return getDatabase().connection;
    }

    public static boolean isConnected() {
        try {
            return getConnection().isValid(3);
        } catch (SQLException | DatabaseException e) {
            Logger.error(e);
            return false;
        }
    }
    
    private static Database getDatabase() throws DatabaseException {
        try {
            if (singleton == null) {
                Logger.trace("Singleton is null, connecting database to new singleton...");
                singleton = new Database();
                singleton.connect();
            }
            if (singleton.connection == null) {
                Logger.warn("Database connection is null. Connecting...");
                singleton.connect();
            }
            if (!singleton.connection.isValid(2)) {
                Logger.warn("Database connection invalid. Reconnecting...");
                singleton.connect();
            }
            return singleton;
        } catch (SQLException e) {
            Logger.error(e, "Failed to connect to database.");
            throw new DatabaseException("Failed to connect to database.");
        }
    }
}
