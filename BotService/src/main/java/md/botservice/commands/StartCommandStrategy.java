package md.botservice.commands;

import md.botservice.models.Command;
import md.botservice.models.TelegramCommands;
import md.botservice.utils.KeyboardHelper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.ArrayList;
import java.util.List;

@Component
public class StartCommandStrategy implements CommandStrategy {

    private static final String WEB_APP_URL = "https://DmitriiKaban.github.io/NewsAggregationPlatform/";

    @Override
    public boolean supports(TelegramCommands command) {
        return TelegramCommands.START == command;
    }

    @Override
    public void execute(Command command, AbsSender sender) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(command.chatId()));
        message.setText("👋 *Welcome to NewsBot!* \n\nChoose an option below:");
        message.setParseMode("Markdown");
        message.setReplyMarkup(KeyboardHelper.getMainMenuKeyboard());

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        // ROW 1
        KeyboardRow row1 = new KeyboardRow();
        KeyboardButton webAppBtn = new KeyboardButton("📱 Open News App");
        webAppBtn.setWebApp(new WebAppInfo(WEB_APP_URL));
        row1.add(webAppBtn);
        keyboard.add(row1);

        // ROW 2
        KeyboardRow row2 = new KeyboardRow();
        row2.add("📚 My Sources");
        row2.add("🎯 My Interests");
        keyboard.add(row2);

        // ROW 3
        KeyboardRow row3 = new KeyboardRow();
        row3.add("❓ Help");
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);

        try {
            sender.execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}