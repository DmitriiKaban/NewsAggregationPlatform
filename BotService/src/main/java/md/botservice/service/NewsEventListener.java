package md.botservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NewsEventListener {

    private final TelegramBotService botService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NewsEventListener(TelegramBotService botService) {
        this.botService = botService;
    }

    @KafkaListener(topics = "news.notification", groupId = "newsbot-notification-group")
    public void consumeNotification(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);

            Long userId = json.get("userId").asLong();
            String title = json.get("title").asText();
            String url = json.get("url").asText();
            double score = json.get("score").asDouble();

            botService.sendNewsAlert(userId, title, url);

        } catch (Exception e) {
            System.err.println("Error sending notification: " + e.getMessage());
        }
    }
}