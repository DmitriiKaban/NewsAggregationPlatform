package md.botservice.repository;

import md.botservice.dto.TopSourceProjection;
import md.botservice.models.Source;
import md.botservice.models.SourceType;
import md.botservice.models.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class SourceRepositoryTest {

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should find source by exact URL")
    void findByUrl_Success() {
        // Arrange
        Source source = new Source();
        source.setUrl("https://t.me/tech_news");
        source.setName("Tech News");
        source.setType(SourceType.TELEGRAM);
        entityManager.persist(source);
        entityManager.flush();

        // Act
        Optional<Source> found = sourceRepository.findByUrl("https://t.me/tech_news");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("Tech News", found.get().getName());
    }

    @Test
    @DisplayName("Should return empty when URL does not exist")
    void findByUrl_NotFound() {
        // Act
        Optional<Source> found = sourceRepository.findByUrl("https://t.me/unknown");

        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("Should return top sources ordered by subscriber count")
    void getTopSources_Success() {
        // Arrange: Create 3 sources
        Source source1 = createSource("https://t.me/s1", "Source 1");
        Source source2 = createSource("https://t.me/s2", "Source 2");
        Source source3 = createSource("https://t.me/s3", "Source 3");

        // Arrange: Create 3 users
        User user1 = createUser(1L);
        User user2 = createUser(2L);
        User user3 = createUser(3L);

        // Source 1 has 3 subscribers
        user1.getSubscriptions().add(source1);
        user2.getSubscriptions().add(source1);
        user3.getSubscriptions().add(source1);

        // Source 2 has 2 subscribers
        user1.getSubscriptions().add(source2);
        user2.getSubscriptions().add(source2);

        // Source 3 has 1 subscriber
        user1.getSubscriptions().add(source3);

        // Save users (which cascades to user_subscriptions table)
        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.persist(user3);
        entityManager.flush();

        // Act
        List<TopSourceProjection> topSources = sourceRepository.getTopSources();

        // Assert
        assertEquals(3, topSources.size());

        // Verify the exact order (Descending by count)
        assertEquals("Source 1", topSources.get(0).getName());
        assertEquals(3, topSources.get(0).getSubscriberCount());

        assertEquals("Source 2", topSources.get(1).getName());
        assertEquals(2, topSources.get(1).getSubscriberCount());

        assertEquals("Source 3", topSources.get(2).getName());
        assertEquals(1, topSources.get(2).getSubscriberCount());
    }

    private Source createSource(String url, String name) {
        Source source = new Source();
        source.setUrl(url);
        source.setName(name);
        source.setType(SourceType.TELEGRAM);
        return entityManager.persist(source);
    }

    private User createUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setFirstName("User" + id);
        return user;
    }
}