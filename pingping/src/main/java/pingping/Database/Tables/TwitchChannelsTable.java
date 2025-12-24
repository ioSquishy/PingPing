package pingping.Database.Tables;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;

import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Database.OrmObjects.TwitchSub;
import pingping.Exceptions.DatabaseException;

public class TwitchChannelsTable {
    public static final String tableName = "twitch_channels";
    // columns in TwitchSub.java

    public static String tableCreationSql() {
        // language=sql
        final String sql = MessageFormat.format("""
            CREATE TABLE IF NOT EXISTS {0} (
                {1} TEXT UNIQUE NOT NULL, -- broadcaster_id
                {2} TEXT -- eventsub_id
                )""", TwitchChannelsTable.tableName, TwitchSub.BROADCASTER_ID, TwitchSub.EVENTSUB_ID);
        Logger.trace("TwitchChannelsTable table craete SQL: {}", sql);
        return sql;
    }

    protected static void insertEventSubId(@NotNull String broadcaster_id, @NotNull String eventsub_id) throws DatabaseException {
        final String sql = "INSERT OR IGNORE INTO twitch_channels (broadcaster_id, eventsub_id) VALUES (?,?)";
        Logger.trace("SQL: {}\n?: {}, {}", sql, broadcaster_id, eventsub_id);
        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setString(1, broadcaster_id);
            statement.setString(2, eventsub_id);
            statement.executeUpdate();
        } catch (SQLException e) {
            Logger.error(e, "Failed to insert eventsub_id {} with broadcaster_id {} into database.", eventsub_id, broadcaster_id);
            throw new DatabaseException("Failed to insert subscription into database.");
        }
    }

    protected static void setEventSubId(@NotNull String broadcaster_id, @NotNull String eventsub_id) throws DatabaseException {
        final String sql = "UPDATE " + TwitchChannelsTable.tableName +
            " SET " + TwitchSub.EVENTSUB_ID + " = ?" +
            " WHERE " + TwitchSub.BROADCASTER_ID + " = ?";
        Logger.trace("SQL: {}\n?: {}", eventsub_id, broadcaster_id);

        try (PreparedStatement statement = Database.getConnection().prepareStatement(sql)) {
            statement.setString(1, eventsub_id);
            statement.setString(2, broadcaster_id);
            statement.executeUpdate();
            Logger.debug("Updated eventSubIds in database for broadcasters with id {} to {}", broadcaster_id, eventsub_id);
        } catch (SQLException e) {
            Logger.error(e, "Failed to update eventSubIds in database for broadcasters with id {} to {}", broadcaster_id, eventsub_id);
            throw new DatabaseException("Failed to update eventSubIds in database.");
        }
    }
}
