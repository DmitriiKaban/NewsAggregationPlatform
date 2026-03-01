package md.botservice.exceptions;

public class TelegramChannelNotFoundException extends RuntimeException {
    public TelegramChannelNotFoundException(String message) {
        super(message);
    }
}
