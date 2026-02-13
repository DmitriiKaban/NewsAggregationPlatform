package md.botservice.models;

public enum TelegramCommands {
    START("/start"),
    HELP("/help"),
    MY_INTERESTS("/myinterests"),
    ADD_SOURCE("/addsource"),
    REMOVE_SOURCE("/removesource"),
    LIST_SOURCES("/sources"),
    WEBAPP("/webapp");

    TelegramCommands(String str) {}

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
