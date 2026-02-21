package md.botservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import md.botservice.models.Source;
import md.botservice.models.User;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SourceUpdatePublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "user.sources.updated";

    public void publishSourceUpdate(User user) {
        try {
            // Extract source URLs
            List<String> subscriptions = user.getSubscriptions().stream()
                    .map(Source::getUrl)
                    .collect(Collectors.toList());

            List<String> readAllSources = user.getReadAllPostsSources().stream()
                    .map(Source::getUrl)
                    .collect(Collectors.toList());

            // Build message
            Map<String, Object> message = new HashMap<>();
            message.put("userId", user.getId());
            message.put("subscriptions", subscriptions);
            message.put("readAllPostsSources", readAllSources);
            message.put("showOnlySubscribedSources", user.isShowOnlySubscribedSources());

            String json = objectMapper.writeValueAsString(message);

            kafkaTemplate.send(TOPIC, String.valueOf(user.getId()), json);

            log.info("📤 Published source update for user {}: {} subscriptions, {} read-all sources, only-subscribed: {}",
                    user.getId(),
                    subscriptions.size(),
                    readAllSources.size(),
                    user.isShowOnlySubscribedSources());

        } catch (Exception e) {
            log.error("❌ Failed to publish source update for user {}: {}", user.getId(), e.getMessage());
        }
    }
}