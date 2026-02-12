package md.botservice.exceptions;

public class CommandNotSupportedException extends RuntimeException {
    public CommandNotSupportedException(String message) {
        super(message);
    }
}
