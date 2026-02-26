package md.botservice.commands;

import md.botservice.models.Command;
import md.botservice.models.Language;
import md.botservice.models.TelegramCommands;
import md.botservice.service.MessageService;
import md.botservice.service.SourceService;
import md.botservice.utils.KeyboardHelper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Component
public class RemoveSourceCommandStrategy extends AbstractCommandStrategy {

    private final SourceService sourceService;
    private final MessageService messageService;

    public RemoveSourceCommandStrategy(KeyboardHelper keyboardHelper, SourceService sourceService, MessageService messageService) {
        super(keyboardHelper);
        this.sourceService = sourceService;
        this.messageService = messageService;
    }

    @Override
    public boolean supports(TelegramCommands command) {
        return TelegramCommands.REMOVE_SOURCE == command;
    }

    @Override
    public void execute(Command command, AbsSender sender) {
        Language lang = command.user().getLanguage();
        String url = command.commandParam();

        if (url == null || url.isEmpty()) {
            sendForceReply(sender, command.chatId(), messageService.get("source.remove_instruction", lang));
            return;
        }

        try {
            sourceService.unsubscribeUser(command.user(), url);
            sendWithMainMenu(sender, command.chatId(), messageService.get("source.removed_success", lang), lang);
        } catch (Exception e) {
            sendWithMainMenu(sender, command.chatId(), messageService.get("source.remove_failed", lang), lang);
        }
    }
}