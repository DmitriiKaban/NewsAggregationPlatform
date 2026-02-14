package md.botservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import md.botservice.models.User;
import md.botservice.utils.KeyboardHelper;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppData;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebAppDataHandler {

    private final UserService userService;
    private final SourceService sourceService;
    private final ObjectMapper objectMapper;

    public void handleWebAppData(Update update, AbsSender sender) {
        WebAppData webAppData = update.getMessage().getWebAppData();

        if (webAppData == null) return;

        long chatId = update.getMessage().getChatId();
        String data = webAppData.getData();
        var telegramUser = update.getMessage().getFrom();

        log.info("Web app data from user {}: {}", telegramUser.getId(), data);

        try {
            User user = userService.findOrRegister(telegramUser);
            JsonNode json = objectMapper.readTree(data);

            String action = json.path("action").asText();

            switch (action) {
                case "save_interests" -> handleSaveInterests(user, json, chatId, sender);
                case "add_source" -> handleAddSource(user, json, chatId, sender);
                case "remove_source" -> handleRemoveSource(user, json, chatId, sender);
                default -> log.warn("Unknown web app action: {}", action);
            }

        } catch (Exception e) {
            log.error("Error processing web app data", e);
            sendResponse(sender, chatId, "❌ Something went wrong processing your request.");
        }
    }

    private void handleSaveInterests(User user, JsonNode json, long chatId, AbsSender sender) {
        String interests = json.path("interests").asText();

        user.setInterestsRaw(interests);
        userService.updateUser(user);

        sendResponse(sender, chatId,
                "✅ *Interests Updated!*\n\nI'll now look for news about:\n`" + interests + "`");
    }

    private void handleAddSource(User user, JsonNode json, long chatId, AbsSender sender) {
        String url = json.path("url").asText();
        if (url.isEmpty()) url = json.path("source").asText();

        try {
            sourceService.subscribeUser(user, url);
            sendResponse(sender, chatId, "✅ *Source Added!*\n\nNow following: `" + url + "`");
        } catch (Exception e) {
            log.error("Failed to add source via WebApp", e);
            sendResponse(sender, chatId, "❌ Failed to add source. It might be invalid or duplicate.");
        }
    }

    private void handleRemoveSource(User user, JsonNode json, long chatId, AbsSender sender) {
        String url = json.path("url").asText();
        sourceService.unsubscribeUser(user, url);
        sendResponse(sender, chatId, "🗑 Source removed: `" + url + "`");
    }

    private void sendResponse(AbsSender sender, long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");

        message.setReplyMarkup(KeyboardHelper.getMainMenuKeyboard());

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send WebApp confirmation", e);
        }
    }
}