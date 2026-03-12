package md.faf223.airecommendationsservice.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SummaryGeneratorListener {

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = "summary.generate.request", groupId = "ai-summary-gen-group")
    public void generateSummary(String message) {
        try {
            JsonNode request = objectMapper.readTree(message);
            if (request.isTextual()) {
                request = objectMapper.readTree(request.asText());
            }

            long userId = request.path("userId").asLong();
            String type = request.path("type").asText();

            if (userId == 0) return;

            int days = "WEEKLY".equals(type) ? 7 : 1;

            String userVectorSql = "SELECT interests_vector, interests_raw FROM users WHERE id = ?";
            List<Map<String, Object>> userData = jdbcTemplate.queryForList(userVectorSql, userId);

            if (userData.isEmpty() || userData.getFirst().get("interests_vector") == null) return;

            String userVector = userData.getFirst().get("interests_vector").toString();

            String fetchArticlesSql = """
                SELECT title, source_name, url, summary
                FROM articles
                WHERE COALESCE(published_at, processed_at) >= NOW() - CAST(? AS integer) * INTERVAL '1 day'
                ORDER BY vector <=> CAST(? AS vector)
                LIMIT 5
            """;

            List<Map<String, Object>> articles = jdbcTemplate.queryForList(fetchArticlesSql, days, userVector);

            if (articles.isEmpty()) return;

            List<Map<String, Object>> articlePayloads = new ArrayList<>();
            for (Map<String, Object> article : articles) {
                Map<String, Object> dto = new HashMap<>();
                dto.put("title", article.get("title"));
                dto.put("sourceName", article.get("source_name"));
                dto.put("url", article.get("url"));
                dto.put("summary", article.get("summary"));

                articlePayloads.add(dto);
            }

            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("userId", userId);
            eventPayload.put("type", type);
            eventPayload.put("articles", articlePayloads);

            String jsonPayload = objectMapper.writeValueAsString(eventPayload);
            kafkaTemplate.send("user-summaries", String.valueOf(userId), jsonPayload);

            log.info("Sent AI summary back to BotService for user {}", userId);

        } catch (Exception e) {
            log.error("Failed to generate AI summary", e);
        }
    }

}