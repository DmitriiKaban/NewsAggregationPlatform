package md.botservice.utils;

import lombok.RequiredArgsConstructor;
import md.botservice.models.Language;
import md.botservice.service.MessageService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class KeyboardHelper {

    private final MessageService messageService;

    public ReplyKeyboardMarkup getMainMenuKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Row 1: My Sources and My Interests
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("📚 My Sources"));
        row1.add(new KeyboardButton("🎯 My Interests"));
        keyboard.add(row1);

        // Row 2: Help
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("❓ Help"));
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        return keyboardMarkup;
    }

    public ReplyKeyboard getMainMenuKeyboard(Language lang) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Row 1: My Sources and My Interests
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton(messageService.get("button.my_sources", lang)));
        row1.add(new KeyboardButton(messageService.get("button.my_interests", lang)));
        keyboard.add(row1);

        // Row 2: Help
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton(messageService.get("button.help", lang)));
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        return keyboardMarkup;
    }
}