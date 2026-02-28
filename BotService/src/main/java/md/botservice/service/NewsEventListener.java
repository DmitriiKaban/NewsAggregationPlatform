package md.botservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import md.botservice.utils.KeyboardHelper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Service
public class NewsEventListener {

    private final TelegramBotService botService;
    private final KeyboardHelper keyboardHelper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NewsEventListener(TelegramBotService botService, KeyboardHelper keyboardHelper) {
        this.botService = botService;
        this.keyboardHelper = keyboardHelper;
    }

    @KafkaListener(topics = "news.notification", groupId = "newsbot-notification-group")
    public void consumeNotification(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);

            Long userId = json.get("userId").asLong();
            String title = json.get("title").asText();
            String url = json.get("url").asText();

            String postId = json.has("postId") ? json.get("postId").asText() :
                    (json.has("id") ? json.get("id").asText() : String.valueOf(url.hashCode()));

            InlineKeyboardMarkup reactionKeyboard = keyboardHelper.getPostReactionKeyboard(postId);

            botService.sendNewsAlert(userId, title, url, reactionKeyboard);

        } catch (Exception e) {
            System.err.println("Error sending notification: " + e.getMessage());
        }
    }

}