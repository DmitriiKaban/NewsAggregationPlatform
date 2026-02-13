package md.botservice.utils;

import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class KeyboardHelper {

    // Make sure this matches your ACTUAL GitHub URL
    private static final String WEB_APP_URL = "https://DmitriiKaban.github.io/NewsAggregationPlatform/";

    public static ReplyKeyboardMarkup getMainMenuKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        // ROW 1: Web App
        KeyboardRow row1 = new KeyboardRow();
        KeyboardButton webAppBtn = new KeyboardButton("📱 Open News App");
        webAppBtn.setWebApp(new WebAppInfo(WEB_APP_URL));
        row1.add(webAppBtn);
        keyboard.add(row1);

        // ROW 2: Management
        KeyboardRow row2 = new KeyboardRow();
        row2.add("📚 My Sources");
        row2.add("🎯 My Interests");
        keyboard.add(row2);

        // ROW 3: Help
        KeyboardRow row3 = new KeyboardRow();
        row3.add("❓ Help");
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false); // Keep it visible

        return keyboardMarkup;
    }
}
