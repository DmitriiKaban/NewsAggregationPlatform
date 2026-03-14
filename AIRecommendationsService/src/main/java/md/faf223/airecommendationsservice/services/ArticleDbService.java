package md.faf223.airecommendationsservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleDbService {

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, Long> sourceIdCache = new ConcurrentHashMap<>();

    public boolean isDuplicate(String vectorStr, String sourceName, String shortTitle) {
        try {
            List<Map<String, Object>> matches = jdbcTemplate.query(
                    "SELECT source_name, 1 - (vector <=> ?::vector) AS similarity FROM articles ORDER BY vector <=> ?::vector LIMIT 1",
                    (rs, rowNum) -> Map.of("source_name", rs.getString("source_name"), "similarity", rs.getDouble("similarity")),
                    vectorStr, vectorStr
            );

            if (matches.isEmpty()) return false;

            double sim = (Double) matches.getFirst().get("similarity");
            String matchedSource = (String) matches.getFirst().get("source_name");

            if (sim > 0.985) {
                log.info("Exact duplicate skipped: {}", shortTitle);
                return true;
            }
            if (sim > 0.90 && !sourceName.equals(matchedSource)) {
                log.info("Cross-source duplicate skipped: {}", shortTitle);
                return true;
            }
        } catch (Exception e) {
            log.warn("Deduplication check failed: {}", e.getMessage());
        }
        return false;
    }

    public Long insertArticle(String title, String link, String summary, String content, String sourceName, String vectorStr) {
        try {
            return jdbcTemplate.queryForObject(
                    "INSERT INTO articles (title, url, summary, content, source_name, vector) VALUES (?, ?, ?, ?, ?, ?::vector) RETURNING id",
                    Long.class, title, link, summary, content, sourceName, vectorStr
            );
        } catch (DuplicateKeyException e) {
            return null;
        }
    }

    public Long getSourceId(String sourceName) {
        return sourceIdCache.computeIfAbsent(sourceName, name -> {
            try {
                return jdbcTemplate.queryForObject("SELECT id FROM sources WHERE name = ?", Long.class, name);
            } catch (Exception e) {
                return null;
            }
        });
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void deleteOldArticles() {
        try {
            log.info("Starting cleanup of articles older than 8 days...");

            int deletedCount = jdbcTemplate.update(
                    "DELETE FROM articles WHERE processed_at < NOW() - INTERVAL '8 days'"
            );

            log.info("Successfully deleted {} old articles.", deletedCount);
        } catch (Exception e) {
            log.error("Failed to delete old articles: {}", e.getMessage(), e);
        }
    }

}