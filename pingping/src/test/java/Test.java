import java.util.concurrent.ExecutionException;

import pingping.Discord.DiscordAPI;

public class Test {
    public static void main(String[] args) throws InterruptedException {
      try {
        DiscordAPI.getAPI().getOwner().get().get().sendMessage("hi");
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }
}
