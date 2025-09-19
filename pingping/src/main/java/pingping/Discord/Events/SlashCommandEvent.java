package pingping.Discord.Events;

import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;
import org.javacord.api.util.event.ListenerManager;
import org.tinylog.Logger;

import pingping.Discord.DiscordAPI;
import pingping.Discord.Commands.DiscordCommandFactory;

public class SlashCommandEvent extends DiscordEvent {
    public static final String event_name = "SlashCommandEvent";
    static {
        DiscordEventRegistrar.registerEvent(event_name, SlashCommandEvent::new);
    }
    protected SlashCommandEvent() {
        super(event_name);
    }
    @Override
    protected void registerEventListener() {
        registerSlashCommandCreateListener();
    }

    private static ListenerManager<SlashCommandCreateListener> listener = null;
    public static ListenerManager<SlashCommandCreateListener> registerSlashCommandCreateListener() {
        if (listener == null) {
            listener = DiscordAPI.getAPI().addSlashCommandCreateListener(event -> {
                SlashCommandInteraction interaction = event.getSlashCommandInteraction();
                try {
                    DiscordCommandFactory.createCommand(interaction.getCommandName(), interaction).runCommand();
                } catch (IllegalArgumentException | UnsupportedOperationException e) {
                    Logger.error(e);
                } catch (Exception e) {
                    Logger.error(e);
                }
            });
            Logger.trace("Registered SlashCommandCreateListener");
        }
        return listener;
    }
}
