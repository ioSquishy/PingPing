package pingping.Discord;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.tinylog.Logger;

import io.github.cdimascio.dotenv.Dotenv;
import pingping.Discord.Commands.DiscordCommandFactory;
import pingping.Discord.Events.SlashCommandEvent;

public class DiscordAPI {
    private static DiscordApi api = null;

    public static void connect() {
        if (api != null) {
            throw new RuntimeException("DiscordAPI.connect() ran again even though API has already been connected to.");
        }

        api = new DiscordApiBuilder().setToken(Dotenv.load().get("DISCORD_TOKEN")).login().join();
        Logger.debug("Connected to Discord API.");

        DiscordCommandFactory.forceLoadCommandClasses();
        Logger.debug("Discord API command classes loaded.");

        DiscordCommandFactory.registerGlobalCommandsInApi();
        Logger.debug("Registered discord commands in API");

        SlashCommandEvent.registerSlashCommandCreateListener();
        Logger.debug("Registered Discord event listeners.");

        Logger.info("Discord API Connected");
    }

    public static DiscordApi getAPI() {
        return api;
    }

    public static long getBotId() {
        return api.getClientId();
    }
}
