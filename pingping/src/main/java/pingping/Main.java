package pingping;

import org.tinylog.Logger;

import pingping.Database.Database;
import pingping.Discord.DiscordAPI;
import pingping.Discord.Events.ErrorLogEvent;
import pingping.Twitch.TwitchConduit;

public class Main {
    public static final byte INSTANCE_ID = 0;
    public static void main(String[] args) {
        ConsoleCommands.startListenerThread();
        try {
            Database.getConnection();
            TwitchConduit.getConduit();
            DiscordAPI.connect();
            ErrorLogEvent.setDmErrorsStatus(isRunningFromJar());
        } catch (Exception e) {
            Logger.error(e, "Failed to start up successfully. Quitting.");
            System.exit(-1);
        }
    }

    private static boolean isRunningFromJar() {
        boolean state = Main.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath()
            .endsWith(".jar");
        Logger.info("Running from jar: {}", state);
        return state;
    }
}