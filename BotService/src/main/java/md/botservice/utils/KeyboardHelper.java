package md.botservice.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;

import java.util.ArrayList;
import java.util.List;

@Component
public class KeyboardHelper {

    private static String webAppUrl;

    @Value("${telegram.webapp.url:https://DmitriiKaban.github.io/NewsAggregationPlatform/}")
    public void setWebAppUrl(String url) {
        KeyboardHelper.webAppUrl = url;
    }

    public static ReplyKeyboardMarkup getMainMenuKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Row 1: Web App button - THIS IS THE KEY!
        KeyboardRow row1 = new KeyboardRow();
        KeyboardButton webAppButton = new KeyboardButton("🎨 Open Web App");
        webAppButton.setWebApp(new WebAppInfo(webAppUrl));
        row1.add(webAppButton);
        keyboard.add(row1);

        // Row 2: My Sources and My Interests
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("📚 My Sources"));
        row2.add(new KeyboardButton("🎯 My Interests"));
        keyboard.add(row2);

        // Row 3: Help
        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("❓ Help"));
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        return keyboardMarkup;
    }
}