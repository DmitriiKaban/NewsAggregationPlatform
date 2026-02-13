package md.botservice.service;

import lombok.extern.slf4j.Slf4j;
import md.botservice.commands.CommandEffectFactory;
import md.botservice.commands.CommandStrategy;
import md.botservice.models.Command;
import md.botservice.models.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
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
            if (update.hasMessage() && update.getMessage().getWebAppData() != null) {
                webAppDataHandler.handleWebAppData(update, this);
                return;
            }

            String text = null;
            Long chatId = null;
            org.telegram.telegrambots.meta.api.objects.User telegramUser = null;

            if (update.hasCallbackQuery()) {
                var callback = update.getCallbackQuery();
                chatId = callback.getMessage().getChatId();
                telegramUser = callback.getFrom();
                String data = callback.getData();

                if ("CMD_ADD_SOURCE".equals(data)) {
                    text = "/addsource";
                } else if ("CMD_REMOVE_SOURCE".equals(data)) {
                    text = "/removesource";
                }

                answerCallbackQuery(callback.getId());
            }

            else if (update.hasMessage() && update.getMessage().hasText()) {
                var msg = update.getMessage();
                chatId = msg.getChatId();
                telegramUser = msg.getFrom();
                text = msg.getText();

                if (msg.getReplyToMessage() != null) {
                    String question = msg.getReplyToMessage().getText();

                    if (question.contains("Paste the link of the source")) {
                        text = "/addsource " + text;
                    }
                    else if (question.contains("Paste the link to remove")) {
                        text = "/removesource " + text;
                    }
                    else if (question.contains("What are you interested in")) {
                        text = "/myinterests " + text;
                    }
                }

                text = mapButtonTextToCommand(text);
            }

            if (text == null) return;

            User user = userService.findOrRegister(telegramUser);

            Command command = Command.of(user, chatId, text);

            CommandStrategy strategy = commandFactory.getStrategy(command);
            strategy.execute(command, this);

        } catch (Exception e) {
            log.error("Error processing update", e);
        }
    }

    private void answerCallbackQuery(String callbackId) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackId);
        try {
            execute(answer);
        } catch (TelegramApiException e) {
        }
    }

    private String mapButtonTextToCommand(String text) {
        if (text == null) return null;

        return switch (text) {
            case "📚 My Sources" -> "/sources";
            case "🎯 My Interests" -> "/myinterests";
            case "❓ Help" -> "/help";
            default -> text;
        };
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