package md.faf223.airecommendationsservice.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
public class KafkaAiProcessor {

    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbcTemplate;
    private final KafkaTemplate<@NonNull String, @NonNull String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, float[]> topicCentroids = new HashMap<>();

    public KafkaAiProcessor(EmbeddingService embeddingService, JdbcTemplate jdbcTemplate,
                            KafkaTemplate<@NonNull String, @NonNull String> kafkaTemplate, ObjectMapper objectMapper) {
        this.embeddingService = embeddingService;
        this.jdbcTemplate = jdbcTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initializeTopicVectors() {
        log.info("Initializing semantic topic vectors...");

        Map<String, String> topics = Map.ofEntries(
                Map.entry("Technology", "Technology, software development, gadgets, cybersecurity, programming, hardware, consumer electronics, smartphones, artificial intelligence, machine learning, ChatGPT, neural networks, LLMs, generative AI, robotics, OpenAI"),
                Map.entry("Science", "Science, space exploration, NASA, SpaceX, physics, astronomy, research discoveries, biology, chemistry"),
                Map.entry("Finance", "Finance, global economy, stock market, banking, inflation, interest rates, investment, wealth, wall street, cryptocurrency, bitcoin, ethereum, blockchain, web3, decentralized finance, crypto exchanges, tokens"),
                Map.entry("Business", "Business, entrepreneurship, startups, corporate earnings, mergers, acquisitions, venture capital, CEOs"),
                Map.entry("Politics", "Politics, government, elections, parliament, international relations, diplomacy, laws, treaties, geopolitics, campaigns"),
                Map.entry("Crime & Law", "Justice system, courts, police, investigations, lawsuits, legal rulings, crime, supreme court, prosecution"),
                Map.entry("Education", "Schools, universities, higher education, student life, academic research, teaching, learning, edtech, tuition"),
                Map.entry("Health", "Healthcare, medicine, diseases, wellness, hospitals, medical research, pandemics, nutrition, mental health, fitness"),
                Map.entry("Climate", "Climate change, global warming, renewable energy, ecology, conservation, sustainability, natural disasters, green tech"),
                Map.entry("Sports", "Sports, football, tennis, olympics, championships, athletes, tournaments, basketball, racing, boxing"),
                Map.entry("Entertainment", "Movies, music, celebrity news, hollywood, pop culture, television, streaming, art, theater, actors, video games, gaming consoles, esports, game development, PlayStation, Xbox, Nintendo, PC gaming"),
                Map.entry("Transport", "Cars, electric vehicles, Tesla, aviation, transportation, logistics, railways, public transit, auto industry"),
                Map.entry("Lifestyle", "Travel, tourism, food, restaurants, fashion, relationships, culture, hobbies, lifestyle, home design")
        );

        for (Map.Entry<String, String> entry : topics.entrySet()) {
            float[] vector = embeddingService.encode("passage: " + entry.getValue());
            topicCentroids.put(entry.getKey(), vector);
        }

        log.info("Successfully initialized {} topic vectors.", topicCentroids.size());
    }

    private double calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private String predictTopic(float[] articleVector) {
        String bestTopic = "General News";
        double highestSimilarity = -1.0;

        for (Map.Entry<String, float[]> entry : topicCentroids.entrySet()) {
            double similarity = calculateCosineSimilarity(articleVector, entry.getValue());
            if (similarity > highestSimilarity) {
                highestSimilarity = similarity;
                bestTopic = entry.getKey();
            }
        }

        return highestSimilarity > 0.55 ? bestTopic : "General News";
    }

    @KafkaListener(topics = "news.raw", groupId = "ai-news-group")
    public void processNewsStream(String message) {
        try {
            JsonNode article = safeDeserialize(message);
            if (article == null) return;

            String title = article.hasNonNull("title") ? article.get("title").asText() : "Untitled";
            String link = article.hasNonNull("link") ? article.get("link").asText() : null;
            String sourceName = article.hasNonNull("source") ? article.get("source").asText() : null;
            String topic = article.hasNonNull("topic") ? article.get("topic").asText() : "General";
            String summary = article.hasNonNull("summary") ? article.get("summary").asText() : "";
            String content = article.hasNonNull("content") ? article.get("content").asText() : "";

            if (link == null || sourceName == null) {
                log.warn("Skipping article - missing link or source. Link: {}, Source: {}", link, sourceName);
                return;
            }

            String textToVectorize = "passage: " + title + " - " + summary;

            if (textToVectorize.length() > 1000) {
                textToVectorize = textToVectorize.substring(0, 1000);
            }

            float[] newsVector = embeddingService.encode(textToVectorize);
            String vectorStr = Arrays.toString(newsVector);
            String aiPredictedTopic = predictTopic(newsVector);
            String shortenedTitle = title.substring(0, Math.min(title.length(), 50));

            try {
                List<Map<String, Object>> matches = jdbcTemplate.query(
                        "SELECT source_name, 1 - (vector <=> ?::vector) AS similarity FROM articles ORDER BY vector <=> ?::vector LIMIT 1",
                        (rs, rowNum) -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("source_name", rs.getString("source_name"));
                            map.put("similarity", rs.getDouble("similarity"));
                            return map;
                        },
                        vectorStr, vectorStr
                );

                if (!matches.isEmpty()) {
                    double maxSim = (Double) matches.getFirst().get("similarity");
                    String matchedSource = (String) matches.getFirst().get("source_name");

                    if (maxSim > 0.985) {
                        log.info("Exact duplicate detected (Sim: {} > 0.985), skipping: {}", String.format("%.4f", maxSim), shortenedTitle);
                        return;
                    } else if (maxSim > 0.90) {
                        if (!sourceName.equals(matchedSource)) {
                            log.info("Cross-source duplicate detected (Sim: {} > 0.90, matched {}), skipping: {}", String.format("%.4f", maxSim), matchedSource, shortenedTitle);
                            return;
                        } else {
                            log.info("Templated article kept (Sim: {}, Same Source: {}): {}", String.format("%.4f", maxSim), sourceName, shortenedTitle);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Deduplication check failed, proceeding normally: {}", e.getMessage());
            }

            try {
                jdbcTemplate.update("""
                        INSERT INTO articles (title, url, summary, content, source_name, vector)
                        VALUES (?, ?, ?, ?, ?, ?::vector)
                        """, title, link, summary, content, sourceName, vectorStr);
                log.info("Saved article: {}", shortenedTitle);
            } catch (DuplicateKeyException e) {
                log.info("Duplicate URL skipped: {}", shortenedTitle);
                return;
            }

            Long articleDbId;
            try {
                articleDbId = jdbcTemplate.queryForObject("SELECT id FROM articles WHERE url = ?", Long.class, link);
                if (articleDbId == null) {
                    log.error("Article ID returned null for url: {}", link);
                    return;
                }
            } catch (Exception e) {
                log.error("Could not find article ID for url: {}", link);
                return;
            }

            Long sourceId;
            try {
                sourceId = jdbcTemplate.queryForObject(
                        "SELECT id FROM sources WHERE name = ?", Long.class, sourceName);
            } catch (Exception e) {
                log.warn("Source not found in database: {}", sourceName);
                return;
            }

            final long finalArticleDbId = articleDbId;

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
                    sendNotification(userId, title, link, 1.0, "read_all", finalArticleDbId, sourceName, aiPredictedTopic);
                    log.debug("[Scenario 1] Read-All triggered for User {}", userId);
                } else if (strictMode) {
                    if (isSubscribed && similarity > 0.82) {
                        sendNotification(userId, title, link, similarity, "strict_ai", finalArticleDbId, sourceName, aiPredictedTopic);
                        log.debug("[Scenario 2] Strict+AI triggered for User {} (Sim: {})", userId, String.format("%.4f", similarity));
                    }
                } else if (similarity > 0.80) {
                    sendNotification(userId, title, link, similarity, "normal_ai", finalArticleDbId, sourceName, aiPredictedTopic);
                    log.debug("[Scenario 3] Normal AI triggered for User {} (Sim: {})", userId, String.format("%.4f", similarity));
                }
                return null;
            }, sourceId, sourceId, vectorStr);

        } catch (Exception e) {
            log.error("Error processing news stream message.", e);
        }
    }

    @KafkaListener(topics = "user.interests.updated", groupId = "ai-user-group")
    public void listenForUserUpdates(String message) {
        try {
            JsonNode data = safeDeserialize(message);
            if (data == null) return;

            long userId = data.hasNonNull("userId") ? data.get("userId").asLong() : 0;

            if (userId == 0) {
                log.warn("Skipping user interest update - missing userId");
                return;
            }

            log.info("Received manual interest update for user: {}", userId);
            updateUserEmbedding(userId);

        } catch (Exception e) {
            log.error("Error triggering vector update from manual interests", e);
        }
    }

    @KafkaListener(topics = "user.post.reactions", groupId = "ai-reaction-group")
    public void handleUserReaction(String message) {
        try {
            log.debug("handleUserReaction received payload: {}", message);
            JsonNode event = safeDeserialize(message);
            if (event == null) return;

            if (!event.has("userId") || !event.has("postId") || !event.has("reactionType")) {
                log.warn("Missing required fields in reaction payload: {}", message);
                return;
            }

            long userId = event.get("userId").asLong();

            long articleId;
            try {
                articleId = Long.parseLong(event.get("postId").asText());
            } catch (NumberFormatException e) {
                log.warn("Ignoring reaction for old/non-numeric postId: {}", event.get("postId").asText());
                return;
            }

            String reaction = event.get("reactionType").asText().toUpperCase();

            List<String> existingReactions = jdbcTemplate.query(
                    "SELECT reaction_type FROM user_reactions WHERE user_id = ? AND article_id = ?",
                    (rs, rowNum) -> rs.getString("reaction_type"),
                    userId, articleId
            );

            String existingReaction = existingReactions.isEmpty() ? null : existingReactions.getFirst();

            if (reaction.equals(existingReaction)) {
                log.info("User {} already reacted {} to article {}. Ignoring duplicate reaction.", userId, reaction, articleId);
                return;
            }

            if (existingReaction == null) {
                jdbcTemplate.update(
                        "INSERT INTO user_reactions (user_id, article_id, reaction_type, reacted_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                        userId, articleId, reaction
                );
            } else {
                jdbcTemplate.update(
                        "UPDATE user_reactions SET reaction_type = ?, reacted_at = CURRENT_TIMESTAMP WHERE user_id = ? AND article_id = ?",
                        reaction, userId, articleId
                );
            }

            log.info("Persisted {} reaction for user {} on article {}", reaction, userId, articleId);

            updateUserEmbedding(userId);

        } catch (Exception e) {
            log.error("Error handling user reaction", e);
        }
    }

    private void updateUserEmbedding(long userId) {
        try {
            List<String> userInterests = jdbcTemplate.query(
                    "SELECT interests_raw FROM users WHERE id = ?",
                    (rs, rowNum) -> rs.getString("interests_raw"),
                    userId
            );
            String explicitInterests = (userInterests.isEmpty() || userInterests.getFirst() == null) ? "" : userInterests.getFirst();

            List<String> likedTitles = jdbcTemplate.query(
                    "SELECT a.title FROM user_reactions r JOIN articles a ON r.article_id = a.id WHERE r.user_id = ? AND r.reaction_type = 'LIKE' ORDER BY r.reacted_at DESC LIMIT 15",
                    (rs, rowNum) -> rs.getString("title"),
                    userId
            );

            List<String> dislikedTitles = jdbcTemplate.query(
                    "SELECT a.title FROM user_reactions r JOIN articles a ON r.article_id = a.id WHERE r.user_id = ? AND r.reaction_type = 'DISLIKE' ORDER BY r.reacted_at DESC LIMIT 5",
                    (rs, rowNum) -> rs.getString("title"),
                    userId
            );

            StringBuilder promptBuilder = new StringBuilder("query: ");
            promptBuilder.append(explicitInterests);

            if (!likedTitles.isEmpty()) {
                promptBuilder.append(". Interested in topics like: ").append(String.join("; ", likedTitles));
            }

            if (!dislikedTitles.isEmpty()) {
                promptBuilder.append(". Not interested in topics like: ").append(String.join("; ", dislikedTitles));
            }

            String textForModel = promptBuilder.toString();

            if (textForModel.length() > 2000) {
                textForModel = textForModel.substring(0, 2000);
            }

            float[] newVector = embeddingService.encode(textForModel);
            String vectorStr = Arrays.toString(newVector);

            jdbcTemplate.update("UPDATE users SET interests_vector = ?::vector WHERE id = ?", vectorStr, userId);

            log.info("Successfully updated AI embedding vector for user {}", userId);

        } catch (Exception e) {
            log.error("Error calculating new embedding vector for user {}", userId, e);
        }
    }

    private void sendNotification(long userId, String title, String url, double score, String reason, long articleId, String sourceName, String topic) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("userId", userId);
            payload.put("title", title);
            payload.put("url", url);
            payload.put("score", score);
            payload.put("reason", reason);
            payload.put("postId", String.valueOf(articleId));
            payload.put("sourceName", sourceName);
            payload.put("topic", topic);

            kafkaTemplate.send("news.notification", objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("Failed to send notification for user {}: {}", userId, e.getMessage());
        }
    }

    private JsonNode safeDeserialize(String message) {
        try {
            return objectMapper.readTree(message);
        } catch (Exception e) {
            log.error("Failed to deserialize Kafka message: {}", message, e);
            return null;
        }
    }

}