package md.botservice.service;

import lombok.extern.slf4j.Slf4j;
import md.botservice.commands.CommandEffectFactory;
import md.botservice.commands.CommandStrategy;
import md.botservice.models.Command;
import md.botservice.models.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Service
public class TelegramBotService extends TelegramLongPollingBot {

    private final UserService userService;
    private final CommandEffectFactory commandFactory;
    private final WebAppDataHandler webAppDataHandler;
    private final String botUsername;

    public TelegramBotService(
            UserService userService,
            CommandEffectFactory commandFactory,
            WebAppDataHandler webAppDataHandler,
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.token}") String botToken
    ) {
        super(botToken);
        this.userService = userService;
        this.commandFactory = commandFactory;
        this.webAppDataHandler = webAppDataHandler;
        this.botUsername = botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            // Handle Web App data (sent from Mini App)
            if (update.hasMessage() && update.getMessage().getWebAppData() != null) {
                log.info("Received web app data from chat {}", update.getMessage().getChatId());
                webAppDataHandler.handleWebAppData(update, this);
                return;
            }

            // Handle regular text messages and commands
            if (!update.hasMessage() || !update.getMessage().hasText()) {
                return;
            }

            long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            var telegramUser = update.getMessage().getFrom();

            // Handle button texts (from ReplyKeyboard)
            text = mapButtonTextToCommand(text);

            User user = userService.findOrRegister(telegramUser);
            Command command = Command.of(user, chatId, text);

            CommandStrategy strategy = commandFactory.getStrategy(command);
            strategy.execute(command, this);

        } catch (Exception e) {
            log.error("Error processing update", e);
            if (update.hasMessage()) {
                sendErrorMessage(update.getMessage().getChatId());
            }
        }
    }

    /**
     * Map button texts from ReplyKeyboard to actual commands
     */
    private String mapButtonTextToCommand(String text) {
        return switch (text) {
            case "📚 My Sources" -> "/sources";
            case "🎯 My Interests" -> "/myinterests";
            case "❓ Help" -> "/help";
            case "🎨 Open Web App" -> "/webapp";
            default -> text;
        };
    }

    private void sendErrorMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("⚠️ Sorry, I couldn't understand that command. Try /help");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send error message", e);
        }
    }

    public void sendNewsAlert(Long chatId, String title, String url) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setParseMode("HTML");

        String cleanTitle = escapeHtml(title);
        String text = cleanTitle + "\n\n<a href=\"" + url + "\">Read More</a>";
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send news alert", e);
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }
}