package pingping.Discord.Exceptions;

public class InvalidArgumentException extends Exception {
    public final String command_name;

    public InvalidArgumentException() {
        super();
        command_name = null;
    }

    public InvalidArgumentException(String message) {
        super(message);
        command_name = null;
    }

    public InvalidArgumentException(String command_name, String invalid_argument_name) {
        super("Argument {"+invalid_argument_name+"} is invalid.");
        this.command_name = command_name;
    }
}
