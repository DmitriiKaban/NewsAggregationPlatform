package md.botservice.service;

import lombok.RequiredArgsConstructor;
import md.botservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
public class TelegramBotService extends TelegramLongPollingBot {

    private final UserRepository userRepository;
    private final String botUsername;

    public TelegramBotService(
            UserRepository userRepository,
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.token}") String botToken
    ) {
        super(botToken);
        this.userRepository = userRepository;
        this.botUsername = botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println("Received message: " + update.getMessage().getText());
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();

            // Basic command handling
            if (messageText.equals("/start")) {
                registerUser(update.getMessage().getFrom());
                sendMessage(chatId, "Welcome to NewsBot! Type your interests.");
            }
        }
    }

    private void registerUser(org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        // Save user to Postgres via userRepository if not exists
    }

    public void sendNewsAlert(Long chatId, String title, String url) {
        sendMessage(chatId, "🔥 " + title + "\n" + url);
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() { return botUsername; }
}
