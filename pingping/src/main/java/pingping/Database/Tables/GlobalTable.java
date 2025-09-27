package pingping.Database.Tables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Exceptions.DatabaseException;

public class GlobalTable {
    public static final String tableName = "global";
    public static enum Columns {
        INSTANCE_ID("instance_id"),
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

    public static void insertRow(long instance_id) throws DatabaseException {
        final String sql = "INSERT OR IGNORE INTO " + GlobalTable.tableName+"("+Columns.INSTANCE_ID+") VALUES(?)";
        Logger.trace("SQL: {}\n?: {}", sql, instance_id);
        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, instance_id);
            statement.executeUpdate();
            Logger.debug("Inserted instance id {} into {}", instance_id, GlobalTable.tableName);
        } catch (SQLException e) {
            Logger.error(e, "Failed to insert instance id {} into {}", instance_id, GlobalTable.tableName);
            throw new DatabaseException("Failed to store instance id in database.");
        }
    }

    public static int getNumberOfConduits() throws DatabaseException {
        final String sql = "SELECT DISTINCT " + GlobalTable.Columns.TWITCH_CONDUIT_ID +
            " FROM " + GlobalTable.tableName;
        Logger.trace("SQL: {}", sql);
        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            ResultSet result = statement.executeQuery();
            int numRows = 0;
            while (result.next()) {
                numRows++;
            }
            return numRows;
        } catch (SQLException e) {
            Logger.warn(e, "Failed to retrieve number of conduits in database.");
            throw new DatabaseException("Failed to retrieve number of conduits in database.");
        }
    }

    /**
     * @param instance_id
     * @param conduit_id
     * @throws DatabaseException if fails to connect to database or set conduit
     */
    public static void setConduitId(long instance_id, @NotNull String conduit_id) throws DatabaseException {
        final String sql = "UPDATE " + GlobalTable.tableName +
            " SET " + Columns.TWITCH_CONDUIT_ID + " = ?" + 
            " WHERE " + Columns.INSTANCE_ID + " = ?";
        Logger.trace("SQL: {}\n?: {}, {}", sql, conduit_id, instance_id);

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setString(1, conduit_id);
            statement.setLong(2, instance_id);
            statement.executeUpdate();
            Logger.debug("Set conduit id for instance id {} to {}", instance_id, conduit_id);
        } catch (SQLException e) {
            Logger.error(e, "Failed to set conduit id for instance id {} to {}", instance_id, conduit_id);
            throw new DatabaseException("Failed to set conduit id in database.");
        }
    }

    public static void putConduitId(long instance_id, @NotNull String conduit_id) throws DatabaseException {
        if (getConduitId(instance_id) == null) {
            insertRow(instance_id);
        }
        setConduitId(instance_id, conduit_id);
    }

    /**
     * 
     * @param instance
     * @return
     * @throws DatabaseException if fails to connect to database
     */
    public static @Nullable String getConduitId(long instance_id) throws DatabaseException {
        final String sql = "SELECT " + Columns.TWITCH_CONDUIT_ID + 
            " FROM " + GlobalTable.tableName + 
            " WHERE " + Columns.INSTANCE_ID + " = ? " +
            " LIMIT 1";
        Logger.trace("SQL: {}\n?: {}", sql, instance_id);
        
        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, instance_id);
            ResultSet result = statement.executeQuery();
            if (!result.next()) {
                Logger.debug("getConduitId() did not retrieve any results for instance id {}", instance_id);
            }
            return result.getString(Columns.TWITCH_CONDUIT_ID.sqlColumnName);
        } catch (SQLException e) {
            Logger.warn(e, "SQLException when attempting to retrieve conduit id for instance id {}", instance_id);
            throw new DatabaseException("Failed to get conduit id from database.");
        }
    }
}
