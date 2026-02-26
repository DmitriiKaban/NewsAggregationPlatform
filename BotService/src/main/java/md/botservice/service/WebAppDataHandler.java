package md.botservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import md.botservice.models.Language;
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
    private final MessageService messageService;
    private final KeyboardHelper keyboardHelper;

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
            sendResponse(sender, chatId, messageService.get("webapp.error.processing", Language.EN), Language.EN);
        }
    }

    private void handleSaveInterests(User user, JsonNode json, long chatId, AbsSender sender) {
        Language lang = user.getLanguage();
        String interests = json.path("interests").asText();

        user.setInterestsRaw(interests);
        userService.updateUser(user);

        String successMsg = messageService.get("webapp.interests.updated", lang, interests);
        sendResponse(sender, chatId, successMsg, lang);
    }

    private void handleAddSource(User user, JsonNode json, long chatId, AbsSender sender) {
        Language lang = user.getLanguage();
        String url = json.path("url").asText();
        if (url.isEmpty()) url = json.path("source").asText();

        try {
            sourceService.subscribeUser(user, url);
            String successMsg = messageService.get("webapp.source.added", lang, url);
            sendResponse(sender, chatId, successMsg, lang);
        } catch (Exception e) {
            log.error("Failed to add source via WebApp", e);
            sendResponse(sender, chatId, messageService.get("webapp.source.add_failed", lang), lang);
        }
    }

    private void handleRemoveSource(User user, JsonNode json, long chatId, AbsSender sender) {
        Language lang = user.getLanguage();
        String url = json.path("url").asText();

        sourceService.unsubscribeUser(user, url);

        String removedMsg = messageService.get("webapp.source.removed", lang, url);
        sendResponse(sender, chatId, removedMsg, lang);
    }

    private void sendResponse(AbsSender sender, long chatId, String text, Language lang) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("HTML");

        message.setReplyMarkup(keyboardHelper.getMainMenuKeyboard(lang));

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send WebApp confirmation", e);
        }
    }
}