package md.botservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import md.botservice.models.Language;
import md.botservice.models.ReactionType;
import md.botservice.models.User;
import md.botservice.utils.KeyboardHelper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackQueryHandler {

    private final UserStateManager stateManager;
    private final SourceService sourceService;
    private final UserService userService;
    private final EventTrackingService eventTrackingService;
    private final MessageService messageService;
    private final KeyboardHelper keyboardHelper;

    public void handleCallbackQuery(CallbackQuery callbackQuery, AbsSender sender) {
        String data = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        int messageId = callbackQuery.getMessage().getMessageId();
        var telegramUser = callbackQuery.getFrom();

        User user = userService.findOrRegister(telegramUser);

        try {
            if (data.startsWith("LIKE_POST:")) {
                String postId = data.substring("LIKE_POST:".length());
                eventTrackingService.trackReaction(user.getId(), postId, ReactionType.LIKE);
                answerCallbackWithToast(callbackQuery, sender, messageService.get("reaction.like.toast", user.getLanguage()));
                return;
            }

            if (data.startsWith("DISLIKE_POST:")) {
                String postId = data.substring("DISLIKE_POST:".length());
                eventTrackingService.trackReaction(user.getId(), postId, ReactionType.DISLIKE);
                answerCallbackWithToast(callbackQuery, sender, messageService.get("reaction.dislike.toast", user.getLanguage()));
                return;
            }

            if (data.startsWith("LANG_")) {
                handleLanguageSelection(data, user, chatId, messageId, sender);
                answerCallback(callbackQuery, sender);
                return;
            }

            // UPDATE INTERESTS
            if ("update_interests".equals(data)) {
                handleUpdateInterests(user, chatId, sender);
                answerCallback(callbackQuery, sender);
                return;
            }

            // KEEP INTERESTS
            if ("keep_interests".equals(data)) {
                handleKeepInterests(user, chatId, sender);
                answerCallback(callbackQuery, sender);
                return;
            }

            // ADD SOURCE
            if ("CMD_ADD_SOURCE".equals(data)) {
                handleAddSource(user, chatId, sender);
                answerCallback(callbackQuery, sender);
                return;
            }

            // REMOVE SOURCE
            if (data.startsWith("REMOVE_SOURCE:")) {
                String sourceIdStr = data.substring("REMOVE_SOURCE:".length());
                Long sourceId = Long.parseLong(sourceIdStr);
                handleRemoveSource(user, sourceId, chatId, sender);
                answerCallback(callbackQuery, sender);
                return;
            }

            // TOGGLE STRICT MODE
            if ("TOGGLE_STRICT_MODE".equals(data)) {
                handleToggleStrictMode(user, chatId, messageId, sender);
                answerCallback(callbackQuery, sender);
                return;
            }

        } catch (Exception e) {
            log.error("Error handling callback query: {}", data, e);
        }
    }

    private void handleLanguageSelection(String data, User user, long chatId, int messageId, AbsSender sender) {
        String langCode = data.substring(5).toLowerCase(); // "LANG_EN" -> "en"
        Language language = Language.fromCode(langCode);

        user.setLanguage(language);
        userService.updateUser(user);

        log.info("User {} selected language: {}", user.getId(), language.getDisplayName());

        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(String.valueOf(chatId));
        editMessage.setMessageId(messageId);
        editMessage.setText(messageService.get("settings.language_changed", language));

        try {
            sender.execute(editMessage);
        } catch (TelegramApiException e) {
            log.error("Failed to edit message", e);
        }

        sendWelcomeMessage(user, chatId, sender);
    }

    private void sendWelcomeMessage(User user, long chatId, AbsSender sender) {
        Language lang = user.getLanguage();

        String welcomeText = messageService.get("welcome.title", lang) + "\n\n" +
                messageService.get("welcome.description", lang) + "\n\n" +
                messageService.get("welcome.quick_start", lang);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(welcomeText);
        message.setParseMode("HTML");
        message.setReplyMarkup(keyboardHelper.getMainMenuKeyboard(lang));

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send welcome message", e);
        }
    }

    private void handleUpdateInterests(User user, long chatId, AbsSender sender) {
        Language lang = user.getLanguage();
        stateManager.setState(user.getId(), UserStateManager.State.AWAITING_INTERESTS);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(messageService.get("interests.prompt", lang));
        message.setParseMode("HTML");
        message.setReplyMarkup(org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard.builder()
                .forceReply(true)
                .build());

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending interests prompt", e);
        }
    }

    private void handleKeepInterests(User user, long chatId, AbsSender sender) {
        Language lang = user.getLanguage();
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("✅ " + messageService.get("interests.current", lang));

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error confirming interests", e);
        }
    }

    private void handleAddSource(User user, long chatId, AbsSender sender) {
        Language lang = user.getLanguage();
        stateManager.setState(user.getId(), UserStateManager.State.AWAITING_SOURCE_URL);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(messageService.get("sources.add_prompt", lang));
        message.setParseMode("HTML");
        message.setReplyMarkup(org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard.builder()
                .forceReply(true)
                .build());

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending add source prompt", e);
        }
    }

    private void handleRemoveSource(User user, Long sourceId, long chatId, AbsSender sender) {
        Language lang = user.getLanguage();
        try {
            sourceService.unsubscribeUser(user, sourceId);

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(messageService.get("sources.removed", lang));

            sender.execute(message);
        } catch (Exception e) {
            log.error("Error removing source", e);
        }
    }

    private void handleToggleStrictMode(User user, long chatId, int messageId, AbsSender sender) {
        boolean newState = !user.isShowOnlySubscribedSources();
        sourceService.setShowOnlySubscribedSources(user, newState);

        // Refresh settings message
        // (You can implement this to update the settings menu)
    }

    private void answerCallback(CallbackQuery callbackQuery, AbsSender sender) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQuery.getId());

        try {
            sender.execute(answer);
        } catch (TelegramApiException e) {
            log.error("Error answering callback", e);
        }
    }

    private void answerCallbackWithToast(CallbackQuery callbackQuery, AbsSender sender, String text) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQuery.getId());
        answer.setText(text);
        answer.setShowAlert(false);

        try {
            sender.execute(answer);
        } catch (TelegramApiException e) {
            log.error("Error answering callback with toast", e);
        }
    }

}