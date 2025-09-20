package pingping.Exceptions;

public class DiscordApiException extends Exception {
    public DiscordApiException() {
        super();
    }

    public DiscordApiException(String message) {
        super(message);
    }
}
