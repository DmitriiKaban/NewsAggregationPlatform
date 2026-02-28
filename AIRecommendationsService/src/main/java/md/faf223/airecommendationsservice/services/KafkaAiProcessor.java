package md.faf223.airecommendationsservice.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class KafkaAiProcessor {

    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbcTemplate;
    private final KafkaTemplate<@NonNull String, @NonNull String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaAiProcessor(EmbeddingService embeddingService, JdbcTemplate jdbcTemplate,
                            KafkaTemplate<@NonNull String, @NonNull String> kafkaTemplate, ObjectMapper objectMapper) {
        this.embeddingService = embeddingService;
        this.jdbcTemplate = jdbcTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "news.raw", groupId = "ai-news-group")
    public void processNewsStream(String message) {
        try {
            JsonNode article = safeDeserialize(message);
            if (article == null) return;

            String title = article.hasNonNull("title") ? article.get("title").asText() : "Untitled";
            String link = article.hasNonNull("link") ? article.get("link").asText() : null;
            String sourceName = article.hasNonNull("source") ? article.get("source").asText() : null;
            String summary = article.hasNonNull("summary") ? article.get("summary").asText() : "";
            String content = article.hasNonNull("content") ? article.get("content").asText() : "";

            if (link == null || sourceName == null) {
                System.out.println("Skipping article - missing link or source");
                return;
            }

            String textToVectorize = "passage: " + title + " - " + summary;

            if (textToVectorize.length() > 1500) {
                textToVectorize = textToVectorize.substring(0, 1500);
            }

            float[] newsVector = embeddingService.encode(textToVectorize);
            String vectorStr = Arrays.toString(newsVector);

            String shortenedTitle = title.substring(0, Math.min(title.length(), 50));
            try {
                jdbcTemplate.update("""
                        INSERT INTO articles (title, url, summary, content, source_name, vector)
                        VALUES (?, ?, ?, ?, ?, ?::vector)
                        """, title, link, summary, content, sourceName, vectorStr);
                System.out.println("Saved: " + shortenedTitle);
            } catch (DuplicateKeyException e) {
                System.out.println("Duplicate: " + shortenedTitle);
                return;
            }

            Long sourceId = jdbcTemplate.queryForObject(
                    "SELECT id FROM sources WHERE name = ?", Long.class, sourceName);

            if (sourceId == null) {
                System.out.println("Source not found: " + sourceName);
                return;
            }

            String sql = """
                    SELECT
                        u.id AS user_id,
                        EXISTS(SELECT 1 FROM user_read_all_sources ur WHERE ur.user_id = u.id AND ur.source_id = ?) AS is_read_all,
                        EXISTS(SELECT 1 FROM user_subscriptions us WHERE us.user_id = u.id AND us.source_id = ?) AS is_subscribed,
                        u.show_only_subscribed_sources,
                        (1 - (u.interests_vector <=> ?::vector)) AS similarity
                    FROM users u
                    WHERE u.interests_vector IS NOT NULL
                    """;

            jdbcTemplate.query(sql, (rs, rowNum) -> {
                long userId = rs.getLong("user_id");
                boolean isReadAll = rs.getBoolean("is_read_all");
                boolean isSubscribed = rs.getBoolean("is_subscribed");
                boolean strictMode = rs.getBoolean("show_only_subscribed_sources");
                double similarity = rs.getDouble("similarity");

                if (isReadAll) {
                    sendNotification(userId, title, link, 1.0, "read_all");
                    System.out.println("[Scenario 1] Read-All: User " + userId);
                } else if (strictMode) {
                    if (isSubscribed && similarity > 0.82) {
                        sendNotification(userId, title, link, similarity, "strict_ai");
                        System.out.printf("[Scenario 2] Strict+AI: User %d (%.4f)%n", userId, similarity);
                    }
                } else if (similarity > 0.80) {
                    sendNotification(userId, title, link, similarity, "normal_ai");
                    System.out.printf("[Scenario 3] Normal AI: User %d (%.4f)%n", userId, similarity);
                }
                return null;
            }, sourceId, sourceId, vectorStr);

        } catch (Exception e) {
            System.err.println("Error processing news: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @KafkaListener(topics = "user.interests.updated", groupId = "ai-user-group")
    public void listenForUserUpdates(String message) {
        try {
            JsonNode data = safeDeserialize(message);
            if (data == null) return;

            long userId = data.hasNonNull("userId") ? data.get("userId").asLong() : 0;
            String interestsText = data.hasNonNull("interests") ? data.get("interests").asText() : null;

            if (userId == 0 || interestsText == null) {
                System.out.println("Skipping - missing userId or interests");
                return;
            }

            String textForModel = "query: " + interestsText;

            if (textForModel.length() > 1500) {
                textForModel = textForModel.substring(0, 1500);
            }

            float[] vector = embeddingService.encode(textForModel);
            String vectorStr = Arrays.toString(vector);

            jdbcTemplate.update("UPDATE users SET interests_vector = ?::vector WHERE id = ?", vectorStr, userId);
            System.out.println("Updated User " + userId + " Vector");

        } catch (Exception e) {
            System.err.println("Error updating vector: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "user.reaction", groupId = "ai-reaction-group")
    public void handleUserReaction(String message) {
        try {
            JsonNode event = safeDeserialize(message);
            if (event == null) return;

            long userId = event.get("userId").asLong();
            long articleId = event.get("articleId").asLong();
            String reaction = event.get("reactionType").asText();

            if (!"LIKE".equalsIgnoreCase(reaction)) {
                return; // todo: do something about dislikes
            }

            String articleTitle = jdbcTemplate.queryForObject(
                    "SELECT title FROM articles WHERE id = ?", String.class, articleId
            );

            if (articleTitle == null) {
                return;
            }

            String currentInterests = jdbcTemplate.queryForObject(
                    "SELECT interests_raw FROM users WHERE id = ?", String.class, userId
            );

            if (currentInterests == null) {
                currentInterests = "";
            }

            String updatedInterests = currentInterests + ". Also interested in: " + articleTitle;

            if (updatedInterests.length() > 1500) {
                updatedInterests = updatedInterests.substring(updatedInterests.length() - 1500);
            }

            float[] newVector = embeddingService.encode("query: " + updatedInterests);
            String vectorStr = Arrays.toString(newVector);

            jdbcTemplate.update(
                    "UPDATE users SET interests_raw = ?, interests_vector = ?::vector WHERE id = ?",
                    updatedInterests, vectorStr, userId
            );

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void sendNotification(long userId, String title, String url, double score, String reason) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("userId", userId);
            payload.put("title", title);
            payload.put("url", url);
            payload.put("score", score);
            payload.put("reason", reason);
            kafkaTemplate.send("news.notification", objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            System.err.println("Failed to send notification for user " + userId + ": " + e.getMessage());
        }
    }

    private JsonNode safeDeserialize(String message) {
        try {
            return objectMapper.readTree(message);
        } catch (Exception e) {
            System.out.println("Failed to deserialize message");
            return null;
        }
    }

}