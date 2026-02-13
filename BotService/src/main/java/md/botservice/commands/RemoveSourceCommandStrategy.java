package md.botservice.commands;

import lombok.RequiredArgsConstructor;
import md.botservice.models.Command;
import md.botservice.models.TelegramCommands;
import md.botservice.service.SourceService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Component
@RequiredArgsConstructor
public class RemoveSourceCommandStrategy implements CommandStrategy {

    private final SourceService sourceService;

    @Override
    public boolean supports(TelegramCommands command) {
        return TelegramCommands.REMOVE_SOURCE == command;
    }

    @Override
    public void execute(Command command, AbsSender sender) {
        String url = command.commandParam();

        if (url == null || url.isEmpty()) {
            sendMessage(sender, command.chatId(), "⚠️ Укажите ссылку для удаления.\nПример: `/removesource https://t.me/s/durov`");
            return;
        }

        sourceService.unsubscribeUser(command.user(), url);
        sendMessage(sender, command.chatId(), "🗑 Источник удален из ваших подписок.");
    }
}
