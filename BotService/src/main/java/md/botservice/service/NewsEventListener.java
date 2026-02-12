package md.botservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NewsEventListener {

    private final TelegramBotService botService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "news.processed", groupId = "newsbot-core")
    public void consumeProcessedNews(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            String title = json.get("title").asText();
            String link = json.get("link").asText();

            // 2. Log it
            System.out.println("✅ Java received processed news: " + title);

            // 3. Temporary test user id
             botService.sendNewsAlert(11111111L, title, link);

        } catch (Exception e) {
            System.err.println("Error processing Kafka message: " + e.getMessage());
        }
    }
}
