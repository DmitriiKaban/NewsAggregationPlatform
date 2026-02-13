package md.botservice.commands;

import lombok.RequiredArgsConstructor;
import md.botservice.models.Command;
import md.botservice.models.TelegramCommands;
import md.botservice.service.SourceService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Component
@RequiredArgsConstructor
public class AddSourceCommandStrategy implements CommandStrategy {

    private final SourceService sourceService;

    @Override
    public boolean supports(TelegramCommands command) {
        return TelegramCommands.ADD_SOURCE == command;
    }

    @Override
    public void execute(Command command, AbsSender sender) {
        String url = command.commandParam();

        if (url == null || url.isEmpty()) {
            sendMessage(sender, command.chatId(),
                    "⚠️ *Ошибка:* Укажите ссылку.\nПример: `/addsource https://t.me/s/durov`");
            return;
        }

        try {
            sourceService.subscribeUser(command.user(), url);
            sendMessage(sender, command.chatId(),
                    "✅ *Источник добавлен!*\nЯ буду следить за новостями от:\n`" + url + "`");
        } catch (Exception e) {
            sendMessage(sender, command.chatId(), "❌ Не удалось добавить источник. Проверьте ссылку.");
        }
    }
}
