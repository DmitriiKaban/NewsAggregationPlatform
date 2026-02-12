package md.botservice.model;

public enum TelegramCommands {
    START,
    HELP,
    MY_INTERESTS;

    public static TelegramCommands getCommand(String rawCommand) {
        if (rawCommand == null) {
            return null;
        }

        String normalized = rawCommand.startsWith("/")
                ? rawCommand.substring(1)
                : rawCommand;

        normalized = normalized.toUpperCase();

        return switch (normalized) {
            case "START" -> START;
            case "HELP" -> HELP;
            case "MYINTERESTS" -> MY_INTERESTS;
            default -> null;
        };
    }
}
