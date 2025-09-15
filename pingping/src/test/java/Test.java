import pingping.Discord.DiscordAPI;
import pingping.Discord.Commands.*;

public class Test {
    public static void main(String[] args) {
        DiscordAPI.connect();
        // new RegisterTwitchSub(null).getGlobalCommandBuilder().get().createGlobal(DiscordAPI.getAPI()).join();
    }
}
