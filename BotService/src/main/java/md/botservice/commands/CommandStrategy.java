package md.botservice.commands;

import md.botservice.models.Command;
import md.botservice.models.TelegramCommands;
import md.botservice.utils.KeyboardHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface CommandStrategy {

    Logger log = LoggerFactory.getLogger(CommandStrategy.class);
    boolean supports(TelegramCommands command);
    void execute(Command command, AbsSender sender);

    default void sendMessage(AbsSender sender, Long chatId, String text) {
        send(sender, chatId, text, null);
    }

    default void sendMessage(AbsSender sender, Long chatId, String text, ReplyKeyboard keyboard) {
        send(sender, chatId, text, keyboard);
    }

    default void sendWithMainMenu(AbsSender sender, Long chatId, String text) {
        send(sender, chatId, text, KeyboardHelper.getMainMenuKeyboard());
    }

    default void sendForceReply(AbsSender sender, Long chatId, String text) {
        send(sender, chatId, text, new ForceReplyKeyboard(true));
    }

    private void send(AbsSender sender, Long chatId, String text, ReplyKeyboard keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");

        if (keyboard != null) {
            message.setReplyMarkup(keyboard);
        }

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            log.error("❌ Error sending message to chat {}: {}", chatId, e.getMessage());
        }
    }
}