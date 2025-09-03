package pingping.Discord.Commands;

import org.javacord.api.interaction.SlashCommandInteraction;

public abstract class DiscordCommand {
    private final SlashCommandInteraction interaction;
    public final String commandName;

    protected DiscordCommand(String commandName, SlashCommandInteraction interaction) {
        this.interaction = interaction;
        this.commandName = commandName;
    }

    public void runCommand() {
        interaction.createImmediateResponder().setContent("Discord command not coded correctly... Unable to run.").respond();
        throw new UnsupportedOperationException("this method has not been implemented");
    }
}