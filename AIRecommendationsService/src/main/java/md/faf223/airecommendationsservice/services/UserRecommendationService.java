package md.faf223.airecommendationsservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRecommendationService {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;

    public record Candidate(long userId, boolean readAll, boolean subscribed, boolean strictMode, double similarity) {}

    public List<Candidate> findMatchingUsers(Long sourceId, String vectorStr) {
        String sql = """
                SELECT u.id AS user_id,
                       EXISTS(SELECT 1 FROM user_read_all_sources ur WHERE ur.user_id = u.id AND ur.source_id = ?) AS is_read_all,
                       EXISTS(SELECT 1 FROM user_subscriptions us WHERE us.user_id = u.id AND us.source_id = ?) AS is_subscribed,
                       u.show_only_subscribed_sources,
                       (1 - (u.interests_vector <=> ?::vector)) AS similarity
                FROM users u WHERE u.interests_vector IS NOT NULL
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new Candidate(
                rs.getLong("user_id"), rs.getBoolean("is_read_all"), rs.getBoolean("is_subscribed"),
                rs.getBoolean("show_only_subscribed_sources"), rs.getDouble("similarity")
        ), sourceId, sourceId, vectorStr);
    }

    public void saveReaction(long userId, long articleId, String reaction) {
        List<String> existingReactions = jdbcTemplate.query(
                "SELECT reaction_type FROM user_reactions WHERE user_id = ? AND article_id = ?",
                (rs, rowNum) -> rs.getString("reaction_type"),
                userId, articleId
        );

        String existingReaction = existingReactions.isEmpty() ? null : existingReactions.getFirst();

        if (reaction.equals(existingReaction)) {
            log.info("User {} already reacted {} to article {}. Ignoring duplicate.", userId, reaction, articleId);
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

        recalculateUserEmbedding(userId);
    }

    public void recalculateUserEmbedding(long userId) {
        try {
            List<String> userInterests = jdbcTemplate.query(
                    "SELECT interests_raw FROM users WHERE id = ?",
                    (rs, rowNum) -> rs.getString("interests_raw"),
                    userId
            );
            String explicitInterests = (userInterests.isEmpty() || userInterests.getFirst() == null)
                    ? "" : userInterests.getFirst();

            // Get recent LIKE history
            List<String> likedTitles = jdbcTemplate.query(
                    """
                            SELECT a.title FROM user_reactions r
                            JOIN articles a ON r.article_id = a.id
                            WHERE r.user_id = ? AND r.reaction_type = 'LIKE'
                            ORDER BY r.reacted_at DESC LIMIT 15
                            """,
                    (rs, rowNum) -> rs.getString("title"),
                    userId
            );

            // Get recent DISLIKE history
            List<String> dislikedTitles = jdbcTemplate.query(
                    """
                            SELECT a.title FROM user_reactions r
                            JOIN articles a ON r.article_id = a.id
                            WHERE r.user_id = ? AND r.reaction_type = 'DISLIKE'
                            ORDER BY r.reacted_at DESC LIMIT 5
                            """,
                    (rs, rowNum) -> rs.getString("title"),
                    userId
            );

            StringBuilder prompt = new StringBuilder("query: ").append(explicitInterests);

            if (!likedTitles.isEmpty()) {
                prompt.append(". Interested in topics like: ").append(String.join("; ", likedTitles));
            }
            if (!dislikedTitles.isEmpty()) {
                prompt.append(". Not interested in topics like: ").append(String.join("; ", dislikedTitles));
            }

            String textForModel = prompt.length() > 2000
                    ? prompt.substring(0, 2000)
                    : prompt.toString();

            float[] newVector = embeddingService.encode(textForModel);

            jdbcTemplate.update(
                    "UPDATE users SET interests_vector = ?::vector WHERE id = ?",
                    toVectorString(newVector), userId
            );

            log.info("Successfully updated AI embedding vector for user {}", userId);

        } catch (Exception e) {
            log.error("Error updating embedding for user {}", userId, e);
        }
    }

    /**
     * Helper to format float[] for PostgreSQL's pgvector extension.
     */
    private String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        return sb.append("]").toString();
    }
}