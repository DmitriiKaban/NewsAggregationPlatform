package md.botservice.models;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Command(User user, long chatId, String commandText, TelegramCommands telegramCommand, String commandParam) {

    public static Command of(User user, long chatId, String commandText) {
        // ^(/\\w+)  -> Captures the command (like /start or /myinterests)
        // (?:\\s+(.*))?$ -> Non-capturing group for space, then capture the rest as params
        Pattern p = Pattern.compile("^(/\\w+)(?:\\s+(.*))?$", Pattern.DOTALL);
        Matcher m = p.matcher(commandText);

        TelegramCommands cmdType;
        String cmdParam = "";

        if (m.matches()) {
            String rawCommand = m.group(1);
            cmdType = TelegramCommands.getCommand(rawCommand);

            if (m.group(2) != null) {
                cmdParam = m.group(2).trim();
            }
        } else {
            cmdType = null;
        }

        return new Command(user, chatId, commandText, cmdType, cmdParam);
    }
}

