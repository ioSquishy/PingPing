package pingping;

import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Discord.DiscordAPI;
import pingping.Discord.Events.ErrorLogEvent;
import pingping.Twitch.TwitchConduit;
import pingping.Youtube.LivePoller;

public class Main {
    public static void main(String[] args) {
        boolean isRunningFromJar = isRunningFromJar();
        if (!isRunningFromJar) {
            System.setProperty("tinylog.configuration", "pingping/src/main/resources/dev.properties");
        }
        ConsoleCommands.startListenerThread();
        try {
            Database.getConnection();
            TwitchConduit.getConduit();
            DiscordAPI.connect();
            LivePoller.startPolling();
            ErrorLogEvent.setDmErrorsStatus(isRunningFromJar);
        } catch (Exception e) {
            Logger.error(e, "Failed to start up successfully. Quitting.");
            System.exit(-1);
        }
    }

    public static boolean isRunningFromJar() {
        return Main.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath()
            .endsWith(".jar");
    }
}