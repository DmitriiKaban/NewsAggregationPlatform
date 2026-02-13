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
            sendForceReply(sender, command.chatId(), "🔗 *Paste Telegram channel name");
            return;
        }

        try {
            sourceService.subscribeUser(command.user(), url);
            sendSuccessMessage(sender, command.chatId(), "✅ *Source Added!*");
        } catch (Exception e) {
            sendMessage(sender, command.chatId(), "❌ Invalid Link.");
        }
    }

    private void sendSuccessMessage(AbsSender sender, Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");

        message.setReplyMarkup(KeyboardHelper.getMainMenuKeyboard());

        try {
            sender.execute(message);
        } catch (Exception e) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}