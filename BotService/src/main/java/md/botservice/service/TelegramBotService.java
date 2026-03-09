package md.botservice.service;

import lombok.extern.slf4j.Slf4j;
import md.botservice.commands.CommandEffectFactory;
import md.botservice.commands.CommandStrategy;
import md.botservice.events.NewsNotificationEvent;
import md.botservice.models.Command;
import md.botservice.models.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Service
public class TelegramBotService extends TelegramLongPollingBot {

    private final UserService userService;
    private final EventTrackingService eventTrackingService;
    private final UserActivityService activityService;
    private final CommandEffectFactory commandFactory;
    private final WebAppDataHandler webAppDataHandler;
    private final CallbackQueryHandler callbackQueryHandler;
    private final UserStateManager stateManager;
    private final StateMessageHandler stateMessageHandler;
    private final String botUsername;

    public TelegramBotService(
            UserService userService, EventTrackingService eventTrackingService,
            CommandEffectFactory commandFactory,
            WebAppDataHandler webAppDataHandler,
            CallbackQueryHandler callbackQueryHandler,
            UserStateManager stateManager,
            StateMessageHandler stateMessageHandler,
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.token}") String botToken, UserActivityService activityService
    ) {
        super(botToken);
        this.userService = userService;
        this.eventTrackingService = eventTrackingService;
        this.commandFactory = commandFactory;
        this.webAppDataHandler = webAppDataHandler;
        this.callbackQueryHandler = callbackQueryHandler;
        this.stateManager = stateManager;
        this.stateMessageHandler = stateMessageHandler;
        this.botUsername = botUsername;
        this.activityService = activityService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                log.info("Received callback query");
                callbackQueryHandler.handleCallbackQuery(update.getCallbackQuery(), this);

                Long userId = update.getCallbackQuery().getFrom().getId();
                activityService.recordActivity(userId);
                return;
            }

            if (update.hasMessage() && update.getMessage().getWebAppData() != null) {
                log.info("Received web app data from chat {}", update.getMessage().getChatId());
                webAppDataHandler.handleWebAppData(update, this);
                return;
            }

            if (!update.hasMessage() || !update.getMessage().hasText()) {
                return;
            }

            long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            var telegramUser = update.getMessage().getFrom();

            User user = userService.findOrRegister(telegramUser);
            activityService.recordActivity(user.getId());

            if (stateManager.isAwaitingInput(user.getId())) {
                log.info("User {} is awaiting input, current state: {}", user.getId(), stateManager.getState(user.getId()));
                stateMessageHandler.handleStateMessage(user, text, chatId, this);
                return;
            }

            text = mapButtonTextToCommand(text);

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

    private String mapButtonTextToCommand(String text) {
        if (text == null) return "";

        if (text.startsWith("📚")) return "/sources";
        if (text.startsWith("🎯")) return "/myinterests";
        if (text.startsWith("❓")) return "/help";
        if (text.startsWith("🎨")) return "/webapp";
        if (text.startsWith("⚙️")) return "/settings";

        return text;
    }

    private void sendErrorMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Sorry, I couldn't understand that command. Try /help");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send error message", e);
        }
    }

    public void sendNewsAlert(NewsNotificationEvent event, InlineKeyboardMarkup reactionKeyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(event.userId()));
        message.setParseMode("HTML");

        // Build the message using the event data
        String cleanTitle = escapeHtml(event.title());
        String text = cleanTitle + "\n\n<a href=\"" + event.url() + "\">Read More</a>";
        message.setText(text);

        if (reactionKeyboard != null) {
            message.setReplyMarkup(reactionKeyboard);
        }

        try {
            execute(message);

            eventTrackingService.trackArticleShown(
                    event.userId(),
                    event.postId(),
                    event.sourceName(),
                    event.topic()
            );

        } catch (TelegramApiException e) {
            log.error("Failed to send news alert to user {}", event.userId(), e);
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