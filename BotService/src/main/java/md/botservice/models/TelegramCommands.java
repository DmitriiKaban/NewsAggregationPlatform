package md.botservice.models;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum TelegramCommands {
    START("/start"),
    HELP("/help"),
    MY_INTERESTS("/myinterests"),
    ADD_SOURCE("/addsource"),
    REMOVE_SOURCE("/removesource"),
    LIST_SOURCES("/sources"),
    WEBAPP("/webapp"),
    SETTINGS("/settings");

    private final String command;

    private static final Map<String, TelegramCommands> LOOKUP = new HashMap<>();

    static {
        for (TelegramCommands cmd : values()) {
            LOOKUP.put(normalize(cmd.command), cmd);
        }

        LOOKUP.put("LISTSOURCES", LIST_SOURCES);
    }

    TelegramCommands(String command) {
        this.command = command;
    }

    public static TelegramCommands getCommand(String rawCommand) {
        if (rawCommand == null || rawCommand.isBlank()) {
            return null;
        }
        return LOOKUP.get(normalize(rawCommand));
    }

    private static String normalize(String input) {
        return input.trim()
                .replace("/", "")
                .toUpperCase();
    }
}
