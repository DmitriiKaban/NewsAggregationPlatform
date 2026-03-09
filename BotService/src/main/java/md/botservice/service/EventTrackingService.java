package md.botservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import md.botservice.dto.AnalyticsEventDto;
import md.botservice.events.UserReactionEvent;
import md.botservice.models.ReactionType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventTrackingService {

    private final KafkaTemplate<@NonNull String, @NonNull Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOPIC_ANALYTICS = "bot.analytics.events";
    private static final String TOPIC_USER_REACTIONS = "user.post.reactions";

    public void trackArticleShown(Long userId, String postId, String sourceName, String topic) {
        sendAnalyticsEvent(userId, "ARTICLE_SHOWN", postId, Map.of("source", sourceName, "topic", topic));
    }

    public void trackArticleOpened(Long userId, String postId, String sourceName, String topic) {
        sendAnalyticsEvent(userId, "ARTICLE_OPENED", postId, Map.of("source", sourceName, "topic", topic));
    }

    public void trackReaction(Long userId, String postId, ReactionType type) {
        sendAnalyticsEvent(userId, "REACTION_" + type.name(), postId, Map.of());

        try {
            UserReactionEvent aiEvent = new UserReactionEvent(userId, postId, type);
            kafkaTemplate.send(TOPIC_USER_REACTIONS, aiEvent);
            log.info("User {} reacted {} to post {}. AI Event sent.", userId, type, postId);
        } catch (Exception e) {
            log.error("Failed to send AI reaction event to Kafka", e);
        }
    }

    private void sendAnalyticsEvent(Long userId, String type, String refId, Map<String, String> meta) {
        try {
            AnalyticsEventDto event = new AnalyticsEventDto(userId, type, refId, objectMapper.writeValueAsString(meta));
            kafkaTemplate.send(TOPIC_ANALYTICS, event);
        } catch (Exception e) {
            log.error("Failed to track analytics event: {}", type, e);
        }
    }
}