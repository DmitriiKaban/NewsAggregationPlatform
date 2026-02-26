package md.botservice.repository;

import md.botservice.dto.TopSourceProjection;
import md.botservice.models.Source;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SourceRepository extends JpaRepository<Source, Long> {
    Optional<Source> findByUrl(String url);

    @Query(value = """
            SELECT s.name AS name, CAST(COUNT(us.user_id) AS INTEGER) AS subscriberCount
            FROM sources s
            JOIN user_subscriptions us ON s.id = us.source_id
            GROUP BY s.id, s.name
            ORDER BY subscriberCount DESC
            LIMIT 10
            """, nativeQuery = true)
    List<TopSourceProjection> getTopSources();
}
