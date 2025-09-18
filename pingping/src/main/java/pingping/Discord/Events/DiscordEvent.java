package pingping.Discord.Events;

public abstract class DiscordEvent {
    public final String event_name;

    public DiscordEvent(String event_name) {
        this.event_name = event_name;
    }

    public void registerEventListener() {
        throw new UnsupportedOperationException(event_name + " registerEventListener() method is not implemented.");
    }
}
