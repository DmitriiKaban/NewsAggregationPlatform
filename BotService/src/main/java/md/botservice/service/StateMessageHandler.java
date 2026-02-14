package md.botservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import md.botservice.models.User;
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
        log.info("Processing interests input for user {}: {}", user.getId(), text);

        try {
            // Update interests
            userService.updateInterests(user.getId(), text);

            // Clear state
            stateManager.clearState(user.getId());

            // Send confirmation
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("✅ *Interests Updated!*\n\nI'll now look for news about:\n`" + text + "`");
            message.setParseMode("Markdown");
            message.setReplyMarkup(KeyboardHelper.getMainMenuKeyboard());

            sender.execute(message);

            log.info("✅ Successfully updated interests for user {}", user.getId());

        } catch (Exception e) {
            log.error("❌ Failed to update interests for user {}", user.getId(), e);
            stateManager.clearState(user.getId());

            sendError(chatId, "Failed to update interests. Please try again.", sender);
        }
    }

    private void handleSourceInput(User user, String text, Long chatId, AbsSender sender) {
        log.info("Processing source input for user {}: {}", user.getId(), text);

        try {
            // Normalize URL
            String normalizedUrl = normalizeTelegramUrl(text);

            // Add source
            sourceService.subscribeUser(user, normalizedUrl);

            // Clear state
            stateManager.clearState(user.getId());

            // Send confirmation
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("✅ *Source Added!*\n\nI'll monitor news from:\n`" + normalizedUrl + "`");
            message.setParseMode("Markdown");
            message.setReplyMarkup(KeyboardHelper.getMainMenuKeyboard());

            sender.execute(message);

            log.info("✅ Successfully added source for user {}", user.getId());

        } catch (Exception e) {
            log.error("❌ Failed to add source for user {}", user.getId(), e);
            stateManager.clearState(user.getId());

            sendError(chatId, "Failed to add source. Please check the URL and try again.", sender);
        }
    }

    private void sendError(Long chatId, String errorText, AbsSender sender) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("❌ " + errorText);
        message.setReplyMarkup(KeyboardHelper.getMainMenuKeyboard());

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send error message", e);
        }
    }

    private String normalizeTelegramUrl(String input) {
        if (input == null) return "";
        String clean = input.trim();

        // If it's already a full URL, return it
        if (clean.startsWith("http")) {
            return clean;
        }

        // Remove @ if present
        if (clean.startsWith("@")) {
            clean = clean.substring(1);
        }

        // Build Telegram URL
        return "https://t.me/s/" + clean;
    }
}
