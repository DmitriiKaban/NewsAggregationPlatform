package md.faf223.airecommendationsservice.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaNewsListener {

    private final ObjectMapper objectMapper;
    private final EmbeddingService embeddingService;
    private final TopicClassifierService topicClassifier;
    private final ArticleDbService articleDbService;
    private final UserRecommendationService recommendationService;
    private final NotificationProducer notificationProducer;

    @KafkaListener(topics = "news.raw", groupId = "ai-news-group", concurrency = "4")
    public void processNewsStream(String message) {
        try {
            JsonNode article = objectMapper.readTree(message);
            String title = article.path("title").asText("Untitled");
            String link = article.path("link").asText(null);
            String sourceName = article.path("source").asText(null);
            String summary = article.path("summary").asText("");

            if (link == null || sourceName == null) return;

            String text = ("passage: " + title + " - " + summary);
            float[] vector = embeddingService.encode(text.length() > 1000 ? text.substring(0, 1000) : text);
            String vectorStr = topicClassifier.toVectorString(vector);
            String aiTopic = topicClassifier.predictTopic(vector);

            if (articleDbService.isDuplicate(vectorStr, sourceName, title)) return;
            Long articleId = articleDbService.insertArticle(title, link, summary, article.path("content").asText(""), sourceName, vectorStr);
            if (articleId == null) return;
            Long sourceId = articleDbService.getSourceId(sourceName);
            if (sourceId == null) return;

            List<UserRecommendationService.Candidate> candidates = recommendationService.findMatchingUsers(sourceId, vectorStr);
            for (var c : candidates) {
                if (c.readAll()) {
                    notificationProducer.send(c.userId(), title, link, 1.0, "read_all", articleId, sourceName, aiTopic, sourceId);
                } else if (c.strictMode() && c.subscribed() && c.similarity() > 0.82) {
                    notificationProducer.send(c.userId(), title, link, c.similarity(), "strict_ai", articleId, sourceName, aiTopic, sourceId);
                } else if (!c.strictMode() && c.similarity() > 0.80) {
                    notificationProducer.send(c.userId(), title, link, c.similarity(), "normal_ai", articleId, sourceName, aiTopic, sourceId);
                }
            }
        } catch (Exception e) {
            log.error("Error processing news stream", e);
        }
    }

    @KafkaListener(topics = "user.interests.updated", groupId = "ai-user-group")
    public void listenForUserUpdates(String message) {
        try {
            long userId = objectMapper.readTree(message).path("userId").asLong(0);
            if (userId != 0) recommendationService.recalculateUserEmbedding(userId);
        } catch (Exception e) { log.error("Error", e); }
    }

    @KafkaListener(topics = "user.post.reactions", groupId = "ai-reaction-group")
    public void handleUserReaction(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            long userId = event.path("userId").asLong();
            long articleId = event.path("postId").asLong();
            String reaction = event.path("reactionType").asText().toUpperCase();

            recommendationService.saveReaction(userId, articleId, reaction);
        } catch (Exception e) { log.error("Error", e); }
    }

}