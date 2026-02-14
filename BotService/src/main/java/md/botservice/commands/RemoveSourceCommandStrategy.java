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
            sendForceReply(sender, command.chatId(), "🗑 *Paste the link of the source to remove:*");
            return;
        }

        try {
            sourceService.unsubscribeUser(command.user(), url);
            sendWithMainMenu(sender, command.chatId(), "✅ *Source Removed!*");
        } catch (Exception e) {
            sendWithMainMenu(sender, command.chatId(), "⚠️ Could not find that source in your list.");
        }
    }
}