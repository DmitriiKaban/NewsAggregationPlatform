package md.botservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import md.botservice.models.User;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppData;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebAppDataHandler {

    private final UserService userService;
    private final SourceService sourceService;
    private final ObjectMapper objectMapper;

    /**
     * Process data sent from the Mini App via Telegram.WebApp.sendData()
     */
    public void handleWebAppData(Update update, AbsSender sender) {
        // Check if message has web app data
        if (!update.hasMessage() || update.getMessage().getWebAppData() == null) {
            return;
        }

        long chatId = update.getMessage().getChatId();
        WebAppData webAppData = update.getMessage().getWebAppData();
        String data = webAppData.getData();

        log.info("Received web app data: {}", data);

        try {
            tools.jackson.databind.JsonNode json = objectMapper.readTree(data);
            String action = json.get("action").asText();

            User user = userService.findOrRegister(update.getMessage().getFrom());

            switch (action) {
                case "save_interests" -> handleSaveInterests(user, json, chatId, sender);
                case "add_source" -> handleAddSource(user, json, chatId, sender);
                case "remove_source" -> handleRemoveSource(user, json, chatId, sender);
                default -> log.warn("Unknown web app action: {}", action);
            }

        } catch (Exception e) {
            log.error("Error processing web app data", e);
            sendErrorMessage(chatId, sender);
        }
    }

    private void handleSaveInterests(User user, JsonNode json, long chatId, AbsSender sender) {
        String interests = json.get("interests").asText();

        log.info("Updating interests for user {}: {}", user.getId(), interests);

        user.setInterestsRaw(interests);
        userService.updateUser(user);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("✅ *Interests Updated!*\n\nI'll now look for news about:\n`" + interests + "`");
        message.setParseMode("Markdown");

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send confirmation", e);
        }
    }

    private void handleAddSource(User user, JsonNode json, long chatId, AbsSender sender) {
        String url = json.get("url").asText();

        log.info("Adding source for user {}: {}", user.getId(), url);

        try {
            sourceService.subscribeUser(user, url);

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("✅ *Source Added!*\n\nNow following: `" + url + "`");
            message.setParseMode("Markdown");
            sender.execute(message);

        } catch (Exception e) {
            log.error("Failed to add source", e);
            sendErrorMessage(chatId, sender);
        }
    }

    private void handleRemoveSource(User user, JsonNode json, long chatId, AbsSender sender) {
        String url = json.get("url").asText();

        log.info("Removing source for user {}: {}", user.getId(), url);

        sourceService.unsubscribeUser(user, url);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("🗑 Source removed: `" + url + "`");
        message.setParseMode("Markdown");

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send confirmation", e);
        }
    }

    private void sendErrorMessage(long chatId, AbsSender sender) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("❌ Something went wrong. Please try again.");

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send error message", e);
        }
    }
}