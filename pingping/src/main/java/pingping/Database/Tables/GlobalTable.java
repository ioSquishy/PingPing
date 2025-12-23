package pingping.Database.Tables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Exceptions.DatabaseException;

public class GlobalTable {
    public static final String tableName = "global";
    public static enum Columns {
        KEY("key"),
        VALUE("value"); // max of 5 conduits per twitch user account

        public final String sqlColumnName;
        private Columns(String sqlColumnName) {
            this.sqlColumnName = sqlColumnName;
        }
        @Override
        public String toString() {
            return this.sqlColumnName;
        }
    }

    public static String tableCreationSql() {
        // language=sql
        String sql = MessageFormat.format("""
                CREATE TABLE IF NOT EXISTS {0} (
                    {1} TEXT UNIQUE NOT NULL,
                    {2} TEXT,
                    PRIMARY KEY {1}
                )
                """, GlobalTable.tableName, GlobalTable.Columns.KEY, GlobalTable.Columns.VALUE);
        Logger.trace("Global table creation SQL: {}", sql);
        return sql;
    }

    public static void insertKey(@NotNull String key) throws DatabaseException {
        final String sql = "INSERT OR IGNORE INTO global (key) VALUES(?)";
        Logger.trace("SQL: {}\n?: {}", sql, key);

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setString(1, key);
            statement.executeUpdate();
            Logger.debug("Inserted key {} into {}", key, GlobalTable.tableName);
        } catch (SQLException e) {
            Logger.error(e, "Failed to insert key {} into {}", key, GlobalTable.tableName);
            throw new DatabaseException("Failed to store key in database.");
        }
    }

    public static void setValue(@NotNull String key, String value) throws DatabaseException {
        final String sql = "UPDATE global SET value = ? WHERE key = ?";
        Logger.trace("SQL: {}\n?: {}, {}", sql, value, key);

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setString(1, value);
            statement.setString(2, key);
            statement.executeUpdate();
            Logger.debug("Set {} to {}", key, value);
        } catch (SQLException e) {
            Logger.error(e, "Failed to set {} to {}", key, value);
            throw new DatabaseException("Failed to key to value in database.");
        }
    }

    public static @Nullable String getValue(@NotNull String key) throws DatabaseException {
        final String sql = "SELECT value FROM global WHERE key = ?";
        Logger.trace("SQL: {}\n?: {}", sql, key);
        
        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setString(1, key);
            ResultSet result = statement.executeQuery();
            if (!result.next()) {
                Logger.debug("getValue() did not retrieve any results for key {}", key);
            }
            return result.getString(Columns.VALUE.sqlColumnName);
        } catch (SQLException e) {
            Logger.warn(e, "SQLException when attempting to retrieve value for key {}", key);
            throw new DatabaseException("Failed to get value from database.");
        }
    }

    public static void putValue(@NotNull String key, String value) throws DatabaseException {
        if (getValue(key) == null) {
            insertKey(key);
        }
        setValue(key, value);
    }
}
