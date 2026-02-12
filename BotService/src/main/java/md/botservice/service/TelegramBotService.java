package md.botservice.service;

import lombok.extern.slf4j.Slf4j;
import md.botservice.commands.CommandEffectFactory;
import md.botservice.commands.CommandStrategy;
import md.botservice.model.Command;
import md.botservice.model.User;
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
    private final String botUsername;

    public TelegramBotService(
            UserService userService,
            CommandEffectFactory commandFactory,
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.token}") String botToken
    ) {
        super(botToken);
        this.userService = userService;
        this.commandFactory = commandFactory;
        this.botUsername = botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
        var telegramUser = update.getMessage().getFrom();

        try {
            User user = userService.findOrRegister(telegramUser);

            Command command = Command.of(user, chatId, text);

            CommandStrategy strategy = commandFactory.getStrategy(command);
            strategy.execute(command, this);

        } catch (Exception e) {
            log.error("Error processing update", e);
            sendErrorMessage(chatId);
        }
    }

    private void sendErrorMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("⚠️ Sorry, I couldn't understand that command.");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendNewsAlert(Long chatId, String title, String url) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("🔥 *News Alert:*\n" + title + "\n\n" + url);
        message.setParseMode("Markdown");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() { return botUsername; }
}