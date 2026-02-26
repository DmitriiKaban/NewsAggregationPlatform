package md.botservice.commands;

import md.botservice.models.Language;
import md.botservice.utils.KeyboardHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public abstract class AbstractCommandStrategy implements CommandStrategy {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final KeyboardHelper keyboardHelper;

    protected AbstractCommandStrategy(KeyboardHelper keyboardHelper) {
        this.keyboardHelper = keyboardHelper;
    }

    protected void sendMessage(AbsSender sender, Long chatId, String text) {
        send(sender, chatId, text, null);
    }

    protected void sendMessage(AbsSender sender, Long chatId, String text, ReplyKeyboard keyboard) {
        send(sender, chatId, text, keyboard);
    }

    protected void sendWithMainMenu(AbsSender sender, Long chatId, String text, Language lang) {
        send(sender, chatId, text, keyboardHelper.getMainMenuKeyboard(lang));
    }

    protected void sendForceReply(AbsSender sender, Long chatId, String text) {
        send(sender, chatId, text, new ForceReplyKeyboard(true));
    }

    private void send(AbsSender sender, Long chatId, String text, ReplyKeyboard keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("HTML");

        if (keyboard != null) {
            message.setReplyMarkup(keyboard);
        }

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message to chat {}: {}", chatId, e.getMessage());
        }
    }
}
