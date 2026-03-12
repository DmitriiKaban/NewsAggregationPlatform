package md.faf223.airecommendationsservice.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void send(long userId, String title, String url, double score, String reason, long articleId, String sourceName, String topic) {
        try {
            ObjectNode payload = objectMapper.createObjectNode()
                    .put("userId", userId).put("title", title).put("url", url)
                    .put("score", score).put("reason", reason)
                    .put("postId", String.valueOf(articleId))
                    .put("sourceName", sourceName).put("topic", topic);
            kafkaTemplate.send("news.notification", objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("Failed to send notification", e);
        }
    }
}