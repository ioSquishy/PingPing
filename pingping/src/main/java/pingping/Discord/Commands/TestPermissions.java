package pingping.Discord.Commands;

import java.util.Optional;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.tinylog.Logger;

import pingping.Discord.DiscordAPI;

public class TestPermissions extends DiscordCommand {
    public static final String commandName = "testpermissions";
    static {
        DiscordCommandFactory.registerCommand(commandName, TestPermissions::new);
    }
    public TestPermissions(SlashCommandInteraction interaction) {
        super(commandName, interaction);
    }
    @Override
    protected Optional<SlashCommandBuilder> getGlobalCommandBuilder() {
        return Optional.of(new SlashCommandBuilder()
            .setName(commandName)
            .setDescription("Returns whether bot can send messages here or not.")
            .setDefaultEnabledForEveryone()
            .setEnabledInDms(false));
    }
    @Override
    public void runCommand() {
        Logger.trace("{} discord command ran.", commandName);
        ServerTextChannel serverTextChannel = interaction.getChannel().flatMap(TextChannel::asServerTextChannel).orElse(null);
        if (serverTextChannel == null) {
            interaction.createImmediateResponder().setContent("Can only send messages to Server Text Channels").setFlags(MessageFlag.EPHEMERAL).respond();
            return;
        }
        User myself = DiscordAPI.getAPI().getYourself();
        interaction.createImmediateResponder()
            .append("- Send message permission: " + getPermissionStatusEmoji((serverTextChannel.hasPermission(myself, PermissionType.SEND_MESSAGES)))).appendNewLine()
            .append("- View channel permission: " + getPermissionStatusEmoji((serverTextChannel.hasPermission(myself, PermissionType.VIEW_CHANNEL)))).appendNewLine()
            .append("- Embed messages permission: " + getPermissionStatusEmoji((serverTextChannel.hasPermission(myself, PermissionType.EMBED_LINKS)))).appendNewLine()
            .append("- Mention everyone/roles permission: " + getPermissionStatusEmoji((serverTextChannel.hasPermission(myself, PermissionType.MENTION_EVERYONE))))
            .setFlags(MessageFlag.EPHEMERAL)
            .respond();
    }

    private static String getPermissionStatusEmoji(boolean hasPermission) {
        return hasPermission ? ":white_check_mark:" : ":x:";
    }
}
