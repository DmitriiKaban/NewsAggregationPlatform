package md.botservice.repository;

import md.botservice.dto.DauProjection;
import md.botservice.dto.SourceRecommendationProjection;
import md.botservice.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // find users with similar interests (>0.75 cosine similarity)
    // return top tg channels that current user is not subscribed to
    @Query(value = """
        WITH target_user AS (
            SELECT interests_vector FROM users WHERE id = :userId
        ),
        similar_users AS (
            SELECT id 
            FROM users 
            WHERE id != :userId 
              AND interests_vector IS NOT NULL 
              AND (interests_vector <=> (SELECT interests_vector FROM target_user)) < 0.25
        )
        SELECT s.name AS name, s.url AS url, CAST(COUNT(us.user_id) AS INTEGER) AS peerCount
        FROM similar_users su
        JOIN user_subscriptions us ON su.id = us.user_id
        JOIN sources s ON us.source_id = s.id
        WHERE s.id NOT IN (SELECT source_id FROM user_subscriptions WHERE user_id = :userId)
        GROUP BY s.id, s.name, s.url
        ORDER BY peerCount DESC
        LIMIT 5
        """, nativeQuery = true)
    List<SourceRecommendationProjection> getRecommendationsForUser(@Param("userId") Long userId);

    // DAU for the last 7 days
    @Query(value = """
        SELECT TO_CHAR(last_active_at, 'YYYY-MM-DD') AS date, CAST(COUNT(id) AS INTEGER) AS count
        FROM users
        WHERE last_active_at >= CURRENT_DATE - INTERVAL '7 days'
        GROUP BY TO_CHAR(last_active_at, 'YYYY-MM-DD')
        ORDER BY date ASC
        """, nativeQuery = true)
    List<DauProjection> getDailyActiveUsers();
}
