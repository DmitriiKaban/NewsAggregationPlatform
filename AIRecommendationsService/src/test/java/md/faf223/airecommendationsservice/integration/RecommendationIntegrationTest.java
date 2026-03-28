package md.faf223.airecommendationsservice.integration;

import md.faf223.airecommendationsservice.services.ArticleDbService;
import md.faf223.airecommendationsservice.services.EmbeddingService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest
class RecommendationIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16"))
            .withDatabaseName("news_db")
            .withUsername("user")
            .withPassword("password");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @MockitoBean
    private EmbeddingService embeddingService;

    @Autowired
    private ArticleDbService articleDbService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @BeforeAll
    static void setupSchema(@Autowired JdbcTemplate template) {
        template.execute("CREATE EXTENSION IF NOT EXISTS vector;");

        template.execute("""
            CREATE TABLE sources (
                id SERIAL PRIMARY KEY,
                name VARCHAR(255) UNIQUE NOT NULL
            );
        """);

        template.execute("""
            CREATE TABLE articles (
                id SERIAL PRIMARY KEY,
                title TEXT,
                url TEXT,
                summary TEXT,
                content TEXT,
                source_name VARCHAR(255),
                vector vector(1024),
                published_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
        """);

        template.execute("INSERT INTO sources (name) VALUES ('TechCrunch');");
    }

    @Test
    void testContextLoadsAndDbOperations() throws Exception {
        when(embeddingService.encode(anyString())).thenReturn(new float[1024]);

        StringBuilder vectorBuilder = new StringBuilder("[");
        for (int i = 0; i < 1024; i++) {
            vectorBuilder.append(i == 0 ? "0.1" : ",0.0");
        }
        vectorBuilder.append("]");
        String vectorStr = vectorBuilder.toString();

        Long articleId = articleDbService.insertArticle(
                "AI Overlords",
                "http://example.com",
                "Summary",
                "Content",
                "TechCrunch",
                vectorStr
        );
        assertNotNull(articleId);

        boolean isDuplicate = articleDbService.isDuplicate(vectorStr, "TechCrunch", "AI Overlords");
        assertTrue(isDuplicate, "Should detect duplicate using pgvector cosine similarity");

        Long sourceId = articleDbService.getSourceId("TechCrunch");
        assertNotNull(sourceId);
        assertEquals(1L, sourceId);
    }

}