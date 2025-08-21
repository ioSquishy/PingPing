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
            return true;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return false;
    }

    public static void createTables() {
        String serverTable = "CREATE TABLE IF NOT EXISTS servers (" +
            "server_id INTEGER PRIMARY KEY" + 
            ");";
        String twitchTable = "CREATE TABLE IF NOT EXISTS twitch_data (" +
            "server_id INTEGER PRIMARY KEY," +
            "subscriptions TEXT," + 
            "FOREIGN KEY (server_id) REFERENCES servers(server_id) ON DELETE CASCADE" + 
            ");";
        try {
            connection.createStatement().execute(serverTable);
            connection.createStatement().execute(twitchTable);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }
}
