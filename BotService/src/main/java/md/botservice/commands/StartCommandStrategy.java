package md.botservice.commands;

import lombok.RequiredArgsConstructor;
import md.botservice.models.Command;
import md.botservice.models.TelegramCommands;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StartCommandStrategy implements CommandStrategy {

    @Value("${telegram.webapp.url:https://DmitriiKaban.github.io/NewsAggregationPlatform/}")
    private String webAppUrl;

    @Override
    public boolean supports(TelegramCommands command) {
        return TelegramCommands.START == command;
    }

    @Override
    public void execute(Command command, AbsSender sender) {
        String welcomeText = """
                👋 *Welcome to NewsBot!*
                
                I help you stay updated with personalized news from your favorite sources.
                
                *Quick Start:*
                • 🎯 Set your interests
                • 📚 Add news sources  
                • 🎨 Use our Web App for easy management
                
                Choose an option below to get started!
                """;

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(command.chatId()));
        message.setText(welcomeText);
        message.setParseMode("Markdown");
        message.setReplyMarkup(getMainMenuKeyboardWithWebApp());

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * CRITICAL: WebApp button MUST be in ReplyKeyboard (KeyboardButton with WebAppInfo)
     * NOT InlineKeyboard - InlineKeyboard buttons don't pass initData properly!
     */
    private ReplyKeyboardMarkup getMainMenuKeyboardWithWebApp() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Row 1: Web App button (THIS IS CRITICAL - must be KeyboardButton)
        KeyboardRow row1 = new KeyboardRow();
        KeyboardButton webAppButton = new KeyboardButton("🎨 Open Web App");
        webAppButton.setWebApp(new WebAppInfo(webAppUrl)); // This passes initData!
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