import pingping.Discord.Helpers.PingCooldown;

public class Test {
    public static void main(String[] args) throws InterruptedException {
      String id = "hi";
      System.out.println("isOnCooldown: " + PingCooldown.isOnCooldown(id));

      PingCooldown.putOnCooldown(id);
      System.out.println("isOnCooldown: " + PingCooldown.isOnCooldown(id));

      Thread.sleep(1500);
      System.out.println("isOnCooldown: " + PingCooldown.isOnCooldown(id));
    }
}
