package pingping.Database.Tables;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;

import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Exceptions.DatabaseException;

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

    public static String tableCreationSql() {
        // language=sql
        final String sql = MessageFormat.format("""
                CREATE TABLE IF NOT EXISTS {0} (
                    {1} INTEGER UNIQUE NOT NULL, -- server_id
                    PRIMARY KEY ({1})
                )
                """, ServerTable.tableName, ServerTable.Columns.SERVER_ID);
        Logger.trace("Server table create SQL: {}", sql);
        return sql;
    }

    /**
     * will also create a new TwitchTable table with the specified server_id
     * @param server_id
     * @return
     * @throws DatabaseException 
     */
    public static void insertEntry(long server_id) throws DatabaseException {
        final String sql = "INSERT OR IGNORE INTO " + ServerTable.tableName+"("+Columns.SERVER_ID+") VALUES(?)";
        Logger.trace("SQL: {}\n?: {}", sql, server_id);
        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, server_id);
            statement.executeUpdate();
        } catch (SQLException e) {
            Logger.error(e, "Failed to insert server id {} into {} table.", server_id, ServerTable.tableName);
            throw new DatabaseException("Failed to store server id in database.");
        }
    }

    /**
     * WARNING: all entries in TwitchSubTable with this server_id will be deleted (eventsub_id's may be lost but still registered in Twitch API)
     */
    public static void removeEntry(long server_id) throws DatabaseException {
        final String sql = "DELETE FROM " + ServerTable.tableName + " WHERE " + Columns.SERVER_ID + " = ?";
        Logger.trace("SQL: {}\n?: {}", sql, server_id);
        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, server_id);
            statement.executeUpdate();
        } catch (SQLException e) {
            Logger.error(e, "Failed to remove server id {} from {} table.", server_id, ServerTable.tableName);
            throw new DatabaseException("Failed to remove server id from database.");
        }
    }
}
