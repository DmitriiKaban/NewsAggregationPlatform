package md.botservice.repository;

import md.botservice.dto.DauProjection;
import md.botservice.models.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    @Query(value = """
        SELECT 
            TO_CHAR(activity_date, 'YYYY-MM-DD') AS date,
            CAST(COUNT(DISTINCT user_id) AS INTEGER) AS count
        FROM user_activity
        WHERE activity_date >= CURRENT_DATE - INTERVAL '7 days'
        GROUP BY activity_date
        ORDER BY activity_date ASC
        """, nativeQuery = true)
    List<DauProjection> getDailyActiveUsers();

    Optional<UserActivity> findByUserIdAndActivityDate(Long userId, LocalDate activityDate);

    @Modifying
    @Query("UPDATE UserActivity ua SET ua.activityCount = ua.activityCount + 1 WHERE ua.userId = :userId AND ua.activityDate = :date")
    void incrementActivityCount(@Param("userId") Long userId, @Param("date") LocalDate date);
}
