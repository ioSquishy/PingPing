package pingping.Discord.Commands;

import java.util.Optional;

import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;

import pingping.Discord.DiscordAPI;

public class Ping extends DiscordCommand {
    public static final String commandName = "ping";
    static {
        DiscordCommandFactory.registerCommand(commandName, Ping::new);
    }
    public Ping(SlashCommandInteraction interaction) {
        super(commandName, interaction);
    }
    @Override
    protected Optional<SlashCommandBuilder> getGlobalCommandBuilder() {
        return Optional.of(new SlashCommandBuilder()
            .setName(commandName)
            .setDescription("Tests bot connection.")
            .setDefaultEnabledForEveryone()
            .setEnabledInDms(true));
    }
    @Override
    public void runCommand() {
        interaction.createImmediateResponder().setContent("Pong! `" + DiscordAPI.getAPI().getLatestGatewayLatency().toMillis() + "ms`").respond();
    }
}
