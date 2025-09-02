package pingping.Discord.Exceptions;

public class TwitchApiFailException extends Exception {
    public TwitchApiFailException() {
        super();
    }

    public TwitchApiFailException(String message) {
        super(message);
    }
}
