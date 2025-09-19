package pingping.Discord;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.tinylog.Logger;

import io.github.cdimascio.dotenv.Dotenv;
import pingping.Discord.Commands.DiscordCommandFactory;
import pingping.Discord.Events.DiscordEventRegistrar;

public class DiscordAPI {
    private static DiscordApi api = null;

    public static void connectOnlyApi() {
        api = new DiscordApiBuilder().setToken(Dotenv.load().get("DISCORD_TOKEN")).login().join();
        Logger.debug("Connected to Discord API.");
    }

    public static void connect() {
        if (api != null) {
            throw new RuntimeException("DiscordAPI.connect() ran again even though API has already been connected to.");
        }

        connectOnlyApi();

        DiscordCommandFactory.forceLoadCommandClasses();
        DiscordCommandFactory.registerGlobalCommandsInApi();

        DiscordEventRegistrar.forceLoadEventClasses();
        DiscordEventRegistrar.registerEvents();

        // TODO see if there's a way i can push error level logs to my dms

        Logger.info("Discord API Connected");
    }

    /**
     * Will be null if not previously connected.
     * @return Javacord API instance
     */
    public static DiscordApi getAPI() {
        return api;
    }

    public static long getBotId() {
        return api.getClientId();
    }
}
