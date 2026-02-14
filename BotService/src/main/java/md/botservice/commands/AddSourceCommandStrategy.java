package md.botservice.commands;

import lombok.RequiredArgsConstructor;
import md.botservice.models.Command;
import md.botservice.models.TelegramCommands;
import md.botservice.service.SourceService;
import md.botservice.utils.FormatUtils;
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
            sendForceReply(sender, command.chatId(),
                    "*Add a News Source*\n\nReply with a Telegram channel link or name.\n\nExample: `https://t.me/durov` or simply `durov`");
            return;
        }

        try {
            sourceService.subscribeUser(command.user(), url);
            sendWithMainMenu(sender, command.chatId(),
                    "✅ *Source Added!*\n\nI'll monitor news from:\n`" + FormatUtils.escapeMarkdownV2(url) + "`");
        } catch (Exception e) {
            sendWithMainMenu(sender, command.chatId(), "❌ Failed to add source. Please check the URL.");
        }
    }
}