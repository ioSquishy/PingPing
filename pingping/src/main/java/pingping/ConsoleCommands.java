package pingping;

import java.util.Scanner;

import pingping.Database.Database;
import pingping.Discord.DiscordAPI;
import pingping.Twitch.TwitchConduit;

public class ConsoleCommands {
    public static void startListenerThread() {
        Thread consoleListener = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String input = scanner.nextLine();
                switch (input.toLowerCase().strip()) {
                    case "stop":
                        System.out.println("-Stopping application.");
                        scanner.close();
                        System.exit(0);
                        break;
                    case "status":
                        System.out.println("-Database connected: " + Database.isConnected());
                        System.out.println("-Twitch conduit latency: " + TwitchConduit.getLatency() + "ms");
                        System.out.println("-Discord gateway latency: " + DiscordAPI.getDiscordGatewayLatency() + "ms");
                        break;
                    default: System.err.println("Error: Unknown command.");
                }
            }
        });
        consoleListener.setDaemon(true);
        consoleListener.start();
    }
}