package pingping.Discord;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

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

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (api != null) {
                try {
                    api.disconnect().join();
                    System.out.println("Discord API connection closed by shutdown hook.");
                } catch (CancellationException | CompletionException e) {
                    System.err.println("Error closing Discord API connection in shutdown hook.");
                    e.printStackTrace();
                }
            }
        }));
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

        Logger.info("Discord API Connected");
    }

    /**
     * If not previously connected, will connect Discord API and its classes/commands
     * @return Javacord API instance
     */
    public static DiscordApi getAPI() {
        if (api == null) {
            connect();
        }
        return api;
    }

    /**
     * In milliseconds.
     * Expected latency between 30 and 300; anything over 1000 means something is wrong.
     * @return -1 if not connected
     */
    public static long getDiscordGatewayLatency() {
        return api != null ? DiscordAPI.getAPI().getLatestGatewayLatency().toMillis() : -1;
    }

    public static long getBotId() {
        return api.getClientId();
    }
}
