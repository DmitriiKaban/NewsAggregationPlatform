package md.botservice.commands;

import md.botservice.models.Command;
import md.botservice.models.Language;
import md.botservice.models.TelegramCommands;
import md.botservice.service.MessageService;
import md.botservice.utils.KeyboardHelper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Component
public class HelpCommandStrategy extends AbstractCommandStrategy {

    private final MessageService messageService;

    public HelpCommandStrategy(KeyboardHelper keyboardHelper, MessageService messageService) {
        super(keyboardHelper);
        this.messageService = messageService;
    }

    @Override
    public boolean supports(TelegramCommands command) {
        return TelegramCommands.HELP == command;
    }

    @Override
    public void execute(Command command, AbsSender sender) {
        Language lang = command.user().getLanguage();
        String helpText = messageService.get("help.full_text", lang);
        sendWithMainMenu(sender, command.chatId(), helpText, lang);
    }
}