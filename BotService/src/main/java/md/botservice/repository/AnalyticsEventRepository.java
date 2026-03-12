package md.botservice.repository;

import md.botservice.dto.SourceFeedbackProjection;
import md.botservice.dto.TopicReadProjection;
import md.botservice.models.AnalyticsEvent;
import md.botservice.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {

    // Topics with highest total reads across the whole system
    @Query(value = """
            SELECT metadata->>'topic' AS topic, CAST(COUNT(*) AS INTEGER) AS readCount
            FROM analytics_events
            WHERE event_type = 'ARTICLE_OPENED' AND metadata->>'topic' IS NOT NULL
            GROUP BY metadata->>'topic'
            ORDER BY readCount DESC
            LIMIT 10
            """, nativeQuery = true)
    List<TopicReadProjection> getGlobalTopTopics();

    // Source Feedback (Likes/Dislikes per source)
    @Query(value = """
            SELECT 
                a.source_name AS sourceName, 
                CAST(COUNT(*) FILTER (WHERE r.reaction_type = 'LIKE') AS INTEGER) AS likes,
                CAST(COUNT(*) FILTER (WHERE r.reaction_type = 'DISLIKE') AS INTEGER) AS dislikes
            FROM user_reactions r
            JOIN articles a ON r.article_id = a.id
            GROUP BY a.source_name
            ORDER BY likes DESC
            LIMIT 15
            """, nativeQuery = true)
    List<SourceFeedbackProjection> getSourceFeedbackStats();

    @Query(value = """
            WITH counts AS (
                SELECT user_id, metadata->>'topic' AS topic, CAST(COUNT(*) AS FLOAT) as reads
                FROM analytics_events WHERE event_type = 'ARTICLE_OPENED' AND metadata->>'topic' IS NOT NULL
                GROUP BY user_id, metadata->>'topic'
            ),
            totals AS (SELECT user_id, SUM(reads) as tot FROM counts GROUP BY user_id),
            probs AS (SELECT c.user_id, (c.reads / t.tot) as p FROM counts c JOIN totals t ON c.user_id = t.user_id),
            entropy AS (SELECT user_id, -SUM(p * LN(p)) as ent FROM probs GROUP BY user_id)
            SELECT COALESCE(AVG(ent), 0.0) FROM entropy
            """, nativeQuery = true)
    Double getGlobalAverageTopicEntropy();

    @Query(value = """
            WITH lagged AS (
                SELECT user_id, created_at, LAG(created_at) OVER (PARTITION BY user_id ORDER BY created_at) as prev_time
                FROM analytics_events WHERE event_type = 'ARTICLE_OPENED'
            ),
            flags AS (
                SELECT user_id, 
                       created_at, -- 🌟 MISSING HERE TOO
                       CASE WHEN extract(epoch from (created_at - prev_time)) > 1800 OR prev_time IS NULL THEN 1 ELSE 0 END as is_new 
                FROM lagged
            ),
            sessions AS (
                SELECT user_id, SUM(is_new) OVER (PARTITION BY user_id ORDER BY created_at) as session_id 
                FROM flags
            )
            SELECT COALESCE(AVG(cnt), 0.0) FROM (
                SELECT COUNT(*) as cnt FROM sessions GROUP BY user_id, session_id
            ) sc
            """, nativeQuery = true)
    Double getGlobalAverageArticlesPerSession();

    @Query(value = """
            WITH lagged AS (
                SELECT created_at, LAG(created_at) OVER (ORDER BY created_at) as prev_time
                FROM analytics_events WHERE event_type = 'ARTICLE_OPENED' AND user_id = :userId
            ),
            flags AS (
                SELECT created_at, -- 🌟 WE WERE MISSING THIS LINE
                       CASE WHEN extract(epoch from (created_at - prev_time)) > 1800 OR prev_time IS NULL THEN 1 ELSE 0 END as is_new 
                FROM lagged
            ),
            sessions AS (
                SELECT SUM(is_new) OVER (ORDER BY created_at) as session_id 
                FROM flags
            )
            SELECT COALESCE(AVG(cnt), 0.0) FROM (
                SELECT COUNT(*) as cnt FROM sessions GROUP BY session_id
            ) sc
            """, nativeQuery = true)
    Double getUserAverageArticlesPerSession(@Param("userId") Long userId);

    // Get average Topic Entropy across all users
    @Query(value = """
            WITH user_topic_counts AS (
                SELECT user_id, metadata->>'topic' AS topic, CAST(COUNT(*) AS FLOAT) as topic_reads
                FROM analytics_events
                WHERE event_type = 'ARTICLE_OPENED' AND metadata->>'topic' IS NOT NULL
                GROUP BY user_id, metadata->>'topic'
            ),
            user_totals AS (
                SELECT user_id, SUM(topic_reads) as total_reads
                FROM user_topic_counts
                GROUP BY user_id
            ),
            user_probabilities AS (
                SELECT utc.user_id, (utc.topic_reads / ut.total_reads) as p
                FROM user_topic_counts utc
                JOIN user_totals ut ON utc.user_id = ut.user_id
            ),
            user_entropy AS (
                SELECT user_id, -SUM(p * LN(p)) as entropy
                FROM user_probabilities
                GROUP BY user_id
            )
            SELECT COALESCE(AVG(entropy), 0.0) FROM user_entropy
            """, nativeQuery = true)
    Double getAverageTopicEntropy();

    @Query(value = """
            WITH counts AS (
                SELECT metadata->>'topic' AS topic, CAST(COUNT(*) AS FLOAT) as reads
                FROM analytics_events WHERE event_type = 'ARTICLE_OPENED' AND user_id = :userId AND metadata->>'topic' IS NOT NULL
                GROUP BY metadata->>'topic'
            ),
            total AS (SELECT SUM(reads) as tot FROM counts),
            probs AS (SELECT (reads / tot) as p FROM counts CROSS JOIN total)
            SELECT COALESCE(-SUM(p * LN(p)), 0.0) FROM probs
            """, nativeQuery = true)
    Double getUserTopicEntropy(@Param("userId") Long userId);

    @Query(value = """
            SELECT COALESCE(
                (COUNT(CASE WHEN event_type = 'ARTICLE_OPENED' THEN 1 END) * 100.0) / 
                NULLIF(COUNT(CASE WHEN event_type = 'ARTICLE_SHOWN' THEN 1 END), 0), 
            0.0)
            FROM analytics_events 
            WHERE user_id = :userId
            """, nativeQuery = true)
    Double getUserCtr(@Param("userId") Long userId);

    // 2. Get Top Interests based on reading history
    @Query(value = """
            SELECT metadata->>'topic' AS topic, CAST(COUNT(*) AS INTEGER) AS count 
            FROM analytics_events 
            WHERE user_id = :userId AND event_type = 'ARTICLE_OPENED' 
            GROUP BY metadata->>'topic' 
            ORDER BY count DESC 
            LIMIT 5
            """, nativeQuery = true)
    List<TopicReadProjection> getTopReadTopics(@Param("userId") Long userId);

    // 3. Get total articles read
    @Query("SELECT COUNT(e) FROM AnalyticsEvent e WHERE e.user.id = :userId AND e.eventType = 'ARTICLE_OPENED'")
    Long getTotalArticlesRead(@Param("userId") Long userId);

    Long user(User user);
}

