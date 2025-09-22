package pingping.Discord.Commands;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Database.OrmObjects.TwitchSub;
import pingping.Discord.Helpers.SubscriptionsEmbed;
import pingping.Exceptions.DatabaseException;

public class ListTwitchSubs extends DiscordCommand {
    public static final String commandName = "listtwitchsubs";
    static {
        DiscordCommandFactory.registerCommand(commandName, ListTwitchSubs::new);
    }
    public ListTwitchSubs(SlashCommandInteraction interaction) {
        super(commandName, interaction);
    }
    @Override
    protected Optional<SlashCommandBuilder> getGlobalCommandBuilder() {
        return Optional.of(new SlashCommandBuilder()
            .setName(commandName)
            .setDescription("List registered twitch subscriptions for this server.")
            .setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR)
            .setEnabledInDms(false));
    }
    @Override
    public void runCommand() {
        InteractionImmediateResponseBuilder response = interaction.createImmediateResponder();
        try {
            Logger.trace("{} discord command ran.", commandName);
            long server_id = this.interaction.getServer().get().getId();
            embedTwitchSubs(server_id).forEach(embed -> response.addEmbed(embed));
            response.respond();
        } catch (NoSuchElementException e) {
            Logger.error(e, "Discord command argument missing for command: {}", commandName);
            response.setContent("Command failed; Missing an argument.").respond();
        } catch (DatabaseException e) {
            Logger.error(e);
            response.setContent(e.getMessage()).respond();
        }
    }

    public static List<EmbedBuilder> embedTwitchSubs(long server_id) throws DatabaseException {
        List<TwitchSub> subs = getTwitchSubs(server_id);
        return SubscriptionsEmbed.embedSubscriptions(subs);
    }

    public static List<TwitchSub> getTwitchSubs(long server_id) throws DatabaseException {
        try {
            return Database.TwitchSubsTable.pullTwitchSubsFromServerId(server_id);
        } catch (DatabaseException e) {
            throw new DatabaseException("Failed to pull Twitch subs from database for this server.");
        }
    }
}
