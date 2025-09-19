package pingping.Discord.Events;

public abstract class DiscordEvent {
    public final String event_name;

    protected DiscordEvent(String event_name) {
        this.event_name = event_name;
    }

    protected void registerEventListener() {
        throw new UnsupportedOperationException(event_name + " registerEventListener() method is not implemented.");
    }
}
