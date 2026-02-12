package md.botservice.service;

import lombok.RequiredArgsConstructor;
import md.botservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
@RequiredArgsConstructor
public class TelegramBotService extends TelegramLongPollingBot {

    private final UserRepository userRepository;

    @Override
    public void onUpdateReceived(Update update) {
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
    public String getBotUsername() { return "YourNewsBotName"; }
    @Override
    public String getBotToken() { return "YOUR_BOT_TOKEN"; }
}
