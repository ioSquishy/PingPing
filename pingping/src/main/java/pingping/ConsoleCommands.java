package pingping;

import java.util.Scanner;

import pingping.Database.Database;
import pingping.Discord.DiscordAPI;
import pingping.Twitch.TwitchAPI;
import pingping.Twitch.TwitchConduit;

public class ConsoleCommands {
    public static void startListenerThread() {
        Thread consoleListener = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String input = scanner.nextLine();
                switch (input.toLowerCase().strip()) {
                    case "help":
                        System.out.println("Available commands:");
                        System.out.println("\"stop\" - stops application completely and cleanly");
                        System.out.println("\"status\" - returns latency and connection statuses");
                        System.out.println("\"delete-conduits\" - deletes all registered conduits from TwitchAPI");
                        break;
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
                    case "delete-conduits":
                        TwitchAPI.deleteAllExistingConduits();
                        break;
                    default: System.err.println("Error: Unknown command. Run \"help\" for available commands.");
                }
            }
        });
        consoleListener.setDaemon(true);
        consoleListener.start();
    }
}