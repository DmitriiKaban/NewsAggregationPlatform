package md.botservice.commands;

import md.botservice.models.Command;
import md.botservice.models.Language;
import md.botservice.models.TelegramCommands;
import md.botservice.service.MessageService;
import md.botservice.service.SourceService;
import md.botservice.utils.FormatUtils;
import md.botservice.utils.KeyboardHelper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Component
public class AddSourceCommandStrategy extends AbstractCommandStrategy {

    private final SourceService sourceService;
    private final MessageService messageService;

    public AddSourceCommandStrategy(KeyboardHelper keyboardHelper, SourceService sourceService, MessageService messageService) {
        super(keyboardHelper);
        this.sourceService = sourceService;
        this.messageService = messageService;
    }

    @Override
    public boolean supports(TelegramCommands command) {
        return TelegramCommands.ADD_SOURCE == command;
    }

    @Override
    public void execute(Command command, AbsSender sender) {
        Language lang = command.user().getLanguage();
        String url = command.commandParam();

        if (url == null || url.isEmpty()) {
            sendForceReply(sender, command.chatId(), messageService.get("source.add_instruction", lang));
            return;
        }

        try {
            sourceService.subscribeUser(command.user(), url);
            String successMsg = messageService.get("sources.added", lang, FormatUtils.escapeHtml(url));
            sendWithMainMenu(sender, command.chatId(), successMsg, lang);
        } catch (Exception e) {
            sendWithMainMenu(sender, command.chatId(), messageService.get("source.add_failed", lang), lang);
        }
    }
}