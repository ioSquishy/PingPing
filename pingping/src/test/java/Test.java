import pingping.Twitch.TwitchAPI;

public class Test {
    public static void main(String[] args) {
        System.out.println("Number of conduits: " + TwitchAPI.getConduitList().size());
        TwitchAPI.deleteAllExistingConduits();
    }
}
