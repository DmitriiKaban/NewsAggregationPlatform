package md.botservice.commands;

import lombok.RequiredArgsConstructor;
import md.botservice.models.Command;
import md.botservice.models.TelegramCommands;
import md.botservice.service.SourceService;
import md.botservice.utils.KeyboardHelper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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
            sendSuccessMessage(sender, command.chatId(), "✅ *Source Removed!*");
        } catch (Exception e) {
            sendSuccessMessage(sender, command.chatId(), "⚠️ Could not find that source in your list.");
        }
    }

    private void sendSuccessMessage(AbsSender sender, Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");
        message.setReplyMarkup(KeyboardHelper.getMainMenuKeyboard()); // Restore buttons

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendForceReply(AbsSender sender, Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");
        message.setReplyMarkup(new ForceReplyKeyboard(true));

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
