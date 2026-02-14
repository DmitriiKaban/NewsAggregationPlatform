package md.botservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import md.botservice.commands.MyInterestsCommandStrategy;
import md.botservice.models.User;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallbackQueryHandler {

    private final MyInterestsCommandStrategy myInterestsCommandStrategy;
    private final UserStateManager stateManager;
    private final UserService userService;
    private final SourceService sourceService;

    public void handleCallbackQuery(CallbackQuery callbackQuery, AbsSender sender) {
        String callbackData = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        String queryId = callbackQuery.getId();
        Long userId = callbackQuery.getFrom().getId();

        log.info("Received callback: {} from chat {}, user {}", callbackData, chatId, userId);

        try {
            if (callbackData.startsWith("REMOVE_SOURCE:")) {
                handleRemoveSource(userId, callbackData, chatId, sender, queryId, callbackQuery.getMessage().getMessageId());
            } else {
                switch (callbackData) {
                    case "update_interests" -> handleUpdateInterests(userId, chatId, sender, queryId, callbackQuery.getMessage().getMessageId());
                    case "keep_interests" -> handleKeepInterests(userId, chatId, sender, queryId, callbackQuery.getMessage().getMessageId());
                    case "CMD_ADD_SOURCE" -> handleAddSource(userId, chatId, sender, queryId, callbackQuery.getMessage().getMessageId());
                    default -> log.warn("Unknown callback data: {}", callbackData);
                }
            }
        } catch (Exception e) {
            log.error("Error handling callback query", e);
        }
    }

    private void handleUpdateInterests(Long userId, Long chatId, AbsSender sender, String queryId, Integer messageId) throws TelegramApiException {
        log.info("Setting user {} state to AWAITING_INTERESTS", userId);

        stateManager.setState(userId, UserStateManager.State.AWAITING_INTERESTS);

        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(queryId);
        answer.setText("Ready to update interests");
        sender.execute(answer);

        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(String.valueOf(chatId));
        editMessage.setMessageId(messageId);
        editMessage.setText("✏️ Updating interests...");
        editMessage.setParseMode("Markdown");
        sender.execute(editMessage);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("🎯 *Enter Your New Interests*\n\nReply to this message with keywords separated by commas.\n\n_Example: AI, Politics MD, Formula 1_");
        message.setParseMode("Markdown");
        message.setReplyMarkup(new ForceReplyKeyboard(true));
        sender.execute(message);

        log.info("✅ User {} state set to AWAITING_INTERESTS", userId);
    }

    private void handleKeepInterests(Long userId, Long chatId, AbsSender sender, String queryId, Integer messageId) throws TelegramApiException {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(queryId);
        answer.setText("Interests kept unchanged ✅");
        sender.execute(answer);

        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(String.valueOf(chatId));
        editMessage.setMessageId(messageId);
        editMessage.setText("✅ *Interests Kept*\n\nYour interests remain unchanged.");
        editMessage.setParseMode("Markdown");
        sender.execute(editMessage);

        myInterestsCommandStrategy.confirmKeepInterests(sender, chatId);
    }

    private void handleAddSource(Long userId, Long chatId, AbsSender sender, String queryId, Integer messageId) throws TelegramApiException {
        log.info("Setting user {} state to AWAITING_SOURCE_URL", userId);

        stateManager.setState(userId, UserStateManager.State.AWAITING_SOURCE_URL);

        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(queryId);
        answer.setText("Ready to add source");
        sender.execute(answer);

        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(String.valueOf(chatId));
        editMessage.setMessageId(messageId);
        editMessage.setText("➕ Adding source...");
        editMessage.setParseMode("Markdown");
        sender.execute(editMessage);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("📡 *Add a News Source*\n\nReply with a Telegram channel link or username.\n\n_Example: https://t.me/durov or @durov_");
        message.setParseMode("Markdown");
        message.setReplyMarkup(new ForceReplyKeyboard(true));
        sender.execute(message);

        log.info("✅ User {} state set to AWAITING_SOURCE_URL", userId);
    }

    private void handleRemoveSource(Long userId, String callbackData, Long chatId, AbsSender sender, String queryId, Integer messageId) throws TelegramApiException {
        // Extract source ID from callback data: "REMOVE_SOURCE:123"
        String[] parts = callbackData.split(":");
        if (parts.length != 2) {
            log.error("Invalid callback data format: {}", callbackData);
            return;
        }

        Long sourceId = Long.parseLong(parts[1]);
        log.info("Removing source {} for user {}", sourceId, userId);

        try {
            User user = userService.findById(userId);
            sourceService.unsubscribeUser(user, sourceId);

            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(queryId);
            answer.setText("✅ Source removed");
            sender.execute(answer);

            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(String.valueOf(chatId));
            editMessage.setMessageId(messageId);
            editMessage.setText("✅ *Source Removed*\n\nThe source has been removed from your subscriptions.");
            editMessage.setParseMode("Markdown");
            sender.execute(editMessage);

            log.info("✅ Successfully removed source {} for user {}", sourceId, userId);

        } catch (Exception e) {
            log.error("❌ Failed to remove source {} for user {}", sourceId, userId, e);

            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(queryId);
            answer.setText("❌ Failed to remove source");
            answer.setShowAlert(true);
            sender.execute(answer);
        }
    }
}