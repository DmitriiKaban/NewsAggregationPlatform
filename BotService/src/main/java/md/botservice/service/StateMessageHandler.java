package md.botservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import md.botservice.models.Language;
import md.botservice.models.User;
import md.botservice.utils.FormatUtils;
import md.botservice.utils.KeyboardHelper;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Service
@RequiredArgsConstructor
public class StateMessageHandler {

    private final UserStateManager stateManager;
    private final UserService userService;
    private final SourceService sourceService;
    private final MessageService messageService;
    private final KeyboardHelper keyboardHelper;

    public void handleStateMessage(User user, String text, Long chatId, AbsSender sender) {
        UserStateManager.State state = stateManager.getState(user.getId());

        log.info("Handling state message for user {}, state: {}, text: {}", user.getId(), state, text);

        switch (state) {
            case AWAITING_INTERESTS -> handleInterestsInput(user, text, chatId, sender);
            case AWAITING_SOURCE_URL -> handleSourceInput(user, text, chatId, sender);
            default -> {
                log.warn("Unknown state: {}", state);
                stateManager.clearState(user.getId());
            }
        }
    }

    private void handleInterestsInput(User user, String text, Long chatId, AbsSender sender) {
        Language lang = user.getLanguage();

        try {
            userService.updateInterests(user.getId(), text);
            stateManager.clearState(user.getId());

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(messageService.get("interests.updated", lang, text));
            message.setParseMode("HTML");

            message.setReplyMarkup(keyboardHelper.getMainMenuKeyboard(lang));

            sender.execute(message);

            log.info("Updated interests for user {}", user.getId());

        } catch (Exception e) {
            log.error("Failed to update interests for user {}", user.getId(), e);
            stateManager.clearState(user.getId());

            String errorText = messageService.get("state.error.interests", lang);
            sendError(chatId, errorText, sender, lang);
        }
    }

    private void handleSourceInput(User user, String text, Long chatId, AbsSender sender) {
        Language lang = user.getLanguage();
        log.info("Processing source input for user {}: {}", user.getId(), text);

        try {
            String normalizedUrl = FormatUtils.normalizeTelegramUrl(text);

            sourceService.subscribeUser(user, normalizedUrl);
            stateManager.clearState(user.getId());

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(messageService.get("sources.added", lang, normalizedUrl));
            message.setParseMode("HTML");

            message.setReplyMarkup(keyboardHelper.getMainMenuKeyboard(lang));

            sender.execute(message);

            log.info("Added source for user {}", user.getId());

        } catch (Exception e) {
            log.error("Failed to add source for user {}", user.getId(), e);
            stateManager.clearState(user.getId());

            String errorText = messageService.get("state.error.source", lang);
            sendError(chatId, errorText, sender, lang);
        }
    }

    private void sendError(Long chatId, String errorText, AbsSender sender, Language lang) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(errorText);
        message.setReplyMarkup(keyboardHelper.getMainMenuKeyboard(lang));

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send error message", e);
        }
    }
}