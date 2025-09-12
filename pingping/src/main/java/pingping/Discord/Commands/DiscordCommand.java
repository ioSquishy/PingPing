package pingping.Discord.Commands;

import java.util.Optional;

import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;

public abstract class DiscordCommand {
    protected final SlashCommandInteraction interaction;
    public final String commandName;

    protected DiscordCommand(String commandName, SlashCommandInteraction interaction) {
        this.interaction = interaction;
        this.commandName = commandName;
    }
    
    /**
     * If command is a global command, it will be present. 
     * Otherwise, you can assume the command was created for its specific server within the concrete class.
     */
    protected Optional<SlashCommandBuilder> getGlobalCommandBuilder() {
        throw new UnsupportedOperationException("this method has not been implemented for the command: " + commandName);
    }

    public void runCommand() {
        interaction.createImmediateResponder().setContent("Discord command not coded correctly... Unable to run.").respond();
        throw new UnsupportedOperationException("this method has not been implemented for the command: " + commandName);
    }
}