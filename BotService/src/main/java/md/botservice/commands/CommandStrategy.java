package md.botservice.commands;

import md.botservice.models.Command;
import md.botservice.models.TelegramCommands;
import org.telegram.telegrambots.meta.bots.AbsSender;

public interface CommandStrategy {
    boolean supports(TelegramCommands command);
    void execute(Command command, AbsSender sender);
}