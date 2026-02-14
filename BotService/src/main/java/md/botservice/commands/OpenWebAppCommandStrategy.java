package md.botservice.commands;

import md.botservice.models.Command;
import md.botservice.models.TelegramCommands;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
public class OpenWebAppCommandStrategy implements CommandStrategy {

    @Value("${telegram.webapp.url}")
    private String webAppUrl;

    @Override
    public boolean supports(TelegramCommands command) {
        return TelegramCommands.WEBAPP == command;
    }

    @Override
    public void execute(Command command, AbsSender sender) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(command.chatId()));
        message.setText("🎨 *Launch Web Interface*\n\nClick the button below to manage your preferences in our modern UI:");
        message.setParseMode("Markdown");

        // Create inline keyboard with web app button
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton webAppButton = new InlineKeyboardButton("📱 Open App");
        webAppButton.setWebApp(new WebAppInfo(webAppUrl));
        row.add(webAppButton);

        keyboard.add(row);
        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendMessage(sender, command.chatId(),
                    "❌ Failed to open web app. Please try again later.");
        }
    }
}