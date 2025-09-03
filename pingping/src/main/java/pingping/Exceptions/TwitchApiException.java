package pingping.Exceptions;

public class TwitchApiException extends Exception {
    public TwitchApiException() {
        super();
    }

    public TwitchApiException(String message) {
        super(message);
    }
}
