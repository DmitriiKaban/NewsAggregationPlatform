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
                    "*Add a News Source*\n\nReply with a Telegram channel link or name\\.\n\nExample: `https://t\\.me/durov` or simply `durov`");
            return;
        }

        try {
            sourceService.subscribeUser(command.user(), url);
            sendMessage(sender, command.chatId(),
                    "*✅ Source Added\\!*\n\nI'll monitor news from:\n`" + escapeMarkdown(url) + "`");
        } catch (Exception e) {
            sendMessage(sender, command.chatId(), "❌ Failed to add source\\. Please check the URL\\.");
        }
    }

    private void sendForceReply(AbsSender sender, Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("MarkdownV2");
        message.setReplyMarkup(new ForceReplyKeyboard(true));

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendMessage(AbsSender sender, Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("MarkdownV2");
        message.setReplyMarkup(KeyboardHelper.getMainMenuKeyboard());

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text.replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }
}