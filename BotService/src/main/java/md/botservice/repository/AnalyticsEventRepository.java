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
            WITH lagged AS (
                SELECT user_id, created_at, LAG(created_at) OVER (PARTITION BY user_id ORDER BY created_at) as prev_time
                FROM analytics_events WHERE event_type IN ('ARTICLE_OPENED', 'REACTION_LIKE', 'REACTION_DISLIKE')
            ),
            flags AS (
                SELECT user_id, 
                       created_at,
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
                FROM analytics_events WHERE event_type IN ('ARTICLE_OPENED', 'REACTION_LIKE', 'REACTION_DISLIKE') AND user_id = :userId
            ),
            flags AS (
                SELECT created_at,
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

    // 1. Fixed Global Top Topics
    @Query(value = """
            WITH interacted_articles AS (
                SELECT DISTINCT reference_id 
                FROM analytics_events 
                WHERE event_type IN ('ARTICLE_OPENED', 'REACTION_LIKE', 'REACTION_DISLIKE')
            )
            SELECT e.metadata->>'topic' AS topic, CAST(COUNT(DISTINCT ia.reference_id) AS INTEGER) AS readCount
            FROM analytics_events e
            JOIN interacted_articles ia ON e.reference_id = ia.reference_id
            WHERE e.event_type = 'ARTICLE_SHOWN' AND e.metadata->>'topic' IS NOT NULL
            GROUP BY e.metadata->>'topic'
            ORDER BY readCount DESC
            LIMIT 10
            """, nativeQuery = true)
    List<TopicReadProjection> getGlobalTopTopics();

    // 2. Fixed Global Average Topic Entropy
    @Query(value = """
            WITH interacted_articles AS (
                SELECT DISTINCT user_id, reference_id 
                FROM analytics_events 
                WHERE event_type IN ('ARTICLE_OPENED', 'REACTION_LIKE', 'REACTION_DISLIKE')
            ),
            article_topics AS (
                SELECT DISTINCT reference_id, metadata->>'topic' AS topic
                FROM analytics_events
                WHERE event_type = 'ARTICLE_SHOWN' AND metadata->>'topic' IS NOT NULL
            ),
            user_topic_counts AS (
                SELECT ia.user_id, at.topic, CAST(COUNT(ia.reference_id) AS FLOAT) as topic_reads
                FROM interacted_articles ia
                JOIN article_topics at ON ia.reference_id = at.reference_id
                GROUP BY ia.user_id, at.topic
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
            WITH interacted_articles AS (
                -- Find the IDs of all articles the user interacted with
                SELECT DISTINCT reference_id 
                FROM analytics_events 
                WHERE user_id = :userId AND event_type IN ('ARTICLE_OPENED', 'REACTION_LIKE', 'REACTION_DISLIKE')
            ),
            article_topics AS (
                -- Look up the topic for those specific articles from the ARTICLE_SHOWN event
                SELECT DISTINCT e.reference_id, e.metadata->>'topic' AS topic
                FROM analytics_events e
                JOIN interacted_articles ia ON e.reference_id = ia.reference_id
                WHERE e.user_id = :userId AND e.event_type = 'ARTICLE_SHOWN' AND e.metadata->>'topic' IS NOT NULL
            ),
            counts AS (
                SELECT topic, CAST(COUNT(*) AS FLOAT) as reads
                FROM article_topics 
                GROUP BY topic
            ),
            total AS (SELECT SUM(reads) as tot FROM counts),
            probs AS (SELECT (reads / tot) as p FROM counts CROSS JOIN total)
            SELECT COALESCE(-SUM(p * LN(p)), 0.0) FROM probs
            """, nativeQuery = true)
    Double getUserTopicEntropy(@Param("userId") Long userId);

    @Query(value = """
            SELECT COALESCE(
                (COUNT(DISTINCT CASE WHEN event_type IN ('ARTICLE_OPENED', 'REACTION_LIKE', 'REACTION_DISLIKE') THEN reference_id END) * 100.0) / 
                NULLIF(COUNT(DISTINCT CASE WHEN event_type = 'ARTICLE_SHOWN' THEN reference_id END), 0), 
            0.0)
            FROM analytics_events 
            WHERE user_id = :userId
            """, nativeQuery = true)
    Double getUserCtr(@Param("userId") Long userId);

    @Query(value = """
            WITH interacted_articles AS (
                SELECT DISTINCT reference_id 
                FROM analytics_events 
                WHERE user_id = :userId AND event_type IN ('ARTICLE_OPENED', 'REACTION_LIKE', 'REACTION_DISLIKE')
            )
            SELECT e.metadata->>'topic' AS topic, CAST(COUNT(DISTINCT e.reference_id) AS INTEGER) AS count 
            FROM analytics_events e
            JOIN interacted_articles ia ON e.reference_id = ia.reference_id
            WHERE e.user_id = :userId AND e.event_type = 'ARTICLE_SHOWN' AND e.metadata->>'topic' IS NOT NULL
            GROUP BY e.metadata->>'topic' 
            ORDER BY count DESC 
            LIMIT 5
            """, nativeQuery = true)
    List<TopicReadProjection> getTopReadTopics(@Param("userId") Long userId);

    @Query("SELECT COUNT(e) FROM AnalyticsEvent e WHERE e.user.id = :userId AND e.eventType IN ('ARTICLE_OPENED', 'REACTION_LIKE', 'REACTION_DISLIKE')")
    Long getTotalArticlesRead(@Param("userId") Long userId);

    Long user(User user);
}

