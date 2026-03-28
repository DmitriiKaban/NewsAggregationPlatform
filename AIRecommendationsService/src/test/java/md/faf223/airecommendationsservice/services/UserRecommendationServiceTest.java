package md.faf223.airecommendationsservice.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRecommendationServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private UserRecommendationService userRecommendationService;

    @Test
    void saveReaction_NewReaction_InsertsAndRecalculates() throws Exception {
        long userId = 1L;
        long articleId = 100L;

        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<String>>any(), eq(userId), eq(articleId)))
                .thenReturn(Collections.emptyList());

        when(jdbcTemplate.query(contains("SELECT interests_raw"), ArgumentMatchers.<RowMapper<String>>any(), eq(userId)))
                .thenReturn(List.of("Tech"));
        when(jdbcTemplate.query(contains("reaction_type = 'LIKE'"), ArgumentMatchers.<RowMapper<String>>any(), eq(userId)))
                .thenReturn(List.of("AI News"));
        when(jdbcTemplate.query(contains("reaction_type = 'DISLIKE'"), ArgumentMatchers.<RowMapper<String>>any(), eq(userId)))
                .thenReturn(Collections.emptyList());
        when(embeddingService.encode(anyString())).thenReturn(new float[]{0.1f, 0.2f});

        userRecommendationService.saveReaction(userId, articleId, "LIKE");

        verify(jdbcTemplate, times(1)).update(contains("INSERT INTO user_reactions"), eq(userId), eq(articleId), eq("LIKE"));
        verify(embeddingService, times(1)).encode(anyString());
        verify(jdbcTemplate, times(1)).update(contains("UPDATE users SET interests_vector"), anyString(), eq(userId));
    }

    @Test
    void saveReaction_DuplicateReaction_Ignores() {
        long userId = 1L;
        long articleId = 100L;

        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<String>>any(), eq(userId), eq(articleId)))
                .thenReturn(List.of("LIKE"));

        userRecommendationService.saveReaction(userId, articleId, "LIKE");

        verify(jdbcTemplate, never()).update(anyString(), any(), any(), any());
    }

}