package pingping.Discord.Commands;

import org.javacord.api.interaction.SlashCommandInteraction;
import org.tinylog.Logger;

public abstract class DiscordCommand {
    private final SlashCommandInteraction interaction;

    public DiscordCommand(SlashCommandInteraction interaction) {
        this.interaction = interaction;
    }

    public void runCommand()
}
