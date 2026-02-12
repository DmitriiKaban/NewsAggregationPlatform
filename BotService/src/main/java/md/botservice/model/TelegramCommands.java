package md.botservice.model;

public enum TelegramCommands {
    START,
    HELP,
    MY_INTERESTS,
    ADD_SOURCE,
    REMOVE_SOURCE,
    LIST_SOURCES;

    public static TelegramCommands getCommand(String rawCommand) {
        if (rawCommand == null) return null;
        String normalized = rawCommand.startsWith("/") ? rawCommand.substring(1).toUpperCase() : rawCommand.toUpperCase();

        return switch (normalized) {
            case "START" -> START;
            case "HELP" -> HELP;
            case "MYINTERESTS" -> MY_INTERESTS;
            case "ADDSOURCE" -> ADD_SOURCE;
            case "REMOVESOURCE" -> REMOVE_SOURCE;
            case "SOURCES", "LISTSOURCES" -> LIST_SOURCES;
            default -> null;
        };
    }
}
