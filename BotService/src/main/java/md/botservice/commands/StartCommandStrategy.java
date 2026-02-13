package md.botservice.commands;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import md.botservice.models.Command;
import md.botservice.models.TelegramCommands;

import java.util.ArrayList;
import java.util.List;

@Component
public class StartCommandStrategy implements CommandStrategy {

    @Value("${telegram.webapp.url:https://DmitriiKaban.github.io/NewsAggregationPlatform/}")
    private String webAppUrl;

    @Override
    public boolean supports(TelegramCommands command) {
        return TelegramCommands.START == command;
    }

    @Override
    public void execute(Command command, AbsSender sender) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(command.chatId()));
        message.setParseMode("Markdown");

        String welcomeText = """
            👋 *Welcome to NewsBot!*
            
            I'm your AI-powered news curator. Choose how you'd like to interact:
            
            🎨 *Web Interface* - Modern UI for managing preferences
            ⌨️ *Commands* - Quick text-based controls
            
            Use the buttons below to get started!
            """;

        message.setText(welcomeText);

        // Option 1: Reply keyboard with Web App button (stays visible)
        ReplyKeyboardMarkup keyboardMarkup = createReplyKeyboard();
        message.setReplyMarkup(keyboardMarkup);

        try {
            sender.execute(message);

            // Option 2: Also send an inline keyboard (one-time use)
            sendInlineWebAppButton(sender, command.chatId());

        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private ReplyKeyboardMarkup createReplyKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Row 1: Web App button
        KeyboardRow row1 = new KeyboardRow();
        KeyboardButton webAppBtn = new KeyboardButton("🎨 Open Web App");
        webAppBtn.setWebApp(new WebAppInfo(webAppUrl));
        row1.add(webAppBtn);

        // Row 2: Quick actions
        KeyboardRow row2 = new KeyboardRow();
        row2.add("📚 My Sources");
        row2.add("🎯 My Interests");

        // Row 3: Help
        KeyboardRow row3 = new KeyboardRow();
        row3.add("❓ Help");

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        return keyboardMarkup;
    }

    private void sendInlineWebAppButton(AbsSender sender, Long chatId) throws TelegramApiException {
        SendMessage inlineMessage = new SendMessage();
        inlineMessage.setChatId(String.valueOf(chatId));
        inlineMessage.setText("Or tap here for quick access:");

        InlineKeyboardMarkup inlineMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> inlineKeyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton webAppButton = new InlineKeyboardButton("🚀 Launch App");
        webAppButton.setWebApp(new WebAppInfo(webAppUrl));
        row.add(webAppButton);

        inlineKeyboard.add(row);
        inlineMarkup.setKeyboard(inlineKeyboard);
        inlineMessage.setReplyMarkup(inlineMarkup);

        sender.execute(inlineMessage);
    }
}