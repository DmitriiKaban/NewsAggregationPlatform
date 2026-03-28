package md.faf223.airecommendationsservice.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleDbServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ArticleDbService articleDbService;

    @Test
    void isDuplicate_ExactMatch_ReturnsTrue() {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Map<String, Object>>>any(), anyString(), anyString()))
                .thenReturn(List.of(Map.of("source_name", "TechCrunch", "similarity", 0.99)));

        boolean result = articleDbService.isDuplicate("[0.1, 0.2]", "TechCrunch", "Test Article");
        assertTrue(result);
    }

    @Test
    void isDuplicate_CrossSourceMatch_ReturnsTrue() {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Map<String, Object>>>any(), anyString(), anyString()))
                .thenReturn(List.of(Map.of("source_name", "Wired", "similarity", 0.95)));

        boolean result = articleDbService.isDuplicate("[0.1, 0.2]", "TechCrunch", "Test Article");
        assertTrue(result);
    }

    @Test
    void isDuplicate_NoMatch_ReturnsFalse() {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Map<String, Object>>>any(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        boolean result = articleDbService.isDuplicate("[0.1, 0.2]", "TechCrunch", "Test Article");
        assertFalse(result);
    }

    @Test
    void insertArticle_Success_ReturnsId() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(10L);

        Long id = articleDbService.insertArticle("Title", "url", "summary", "content", "source", "[0.1]");
        assertEquals(10L, id);
    }

    @Test
    void insertArticle_DuplicateKey_ReturnsNull() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(), any(), any(), any(), any(), any()))
                .thenThrow(new DuplicateKeyException("Duplicate"));

        Long id = articleDbService.insertArticle("Title", "url", "summary", "content", "source", "[0.1]");
        assertNull(id);
    }

    @Test
    void deleteOldArticles_ExecutesSuccessfully() {
        when(jdbcTemplate.update(anyString())).thenReturn(5);
        assertDoesNotThrow(() -> articleDbService.deleteOldArticles());
        verify(jdbcTemplate, times(1)).update(anyString());
    }

}