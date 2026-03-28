package md.faf223.airecommendationsservice.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaNewsListenerTest {

    @Mock private ObjectMapper objectMapper;
    @Mock private EmbeddingService embeddingService;
    @Mock private TopicClassifierService topicClassifier;
    @Mock private ArticleDbService articleDbService;
    @Mock private UserRecommendationService recommendationService;
    @Mock private NotificationProducer notificationProducer;

    @InjectMocks
    private KafkaNewsListener kafkaNewsListener;

    @Test
    void processNewsStream_ValidArticle_SendsNotifications() throws Exception {
        String json = "{\"title\":\"Test\",\"link\":\"http://test.com\",\"source\":\"Tech\",\"summary\":\"Sum\"}";
        ObjectMapper realMapper = new ObjectMapper();

        when(objectMapper.readTree(anyString())).thenReturn(realMapper.readTree(json));
        when(embeddingService.encode(anyString())).thenReturn(new float[]{0.1f});
        when(topicClassifier.toVectorString(any())).thenReturn("[0.1]");
        when(topicClassifier.predictTopic(any())).thenReturn("Technology");
        when(articleDbService.isDuplicate(anyString(), anyString(), anyString())).thenReturn(false);
        when(articleDbService.insertArticle(any(), any(), any(), any(), any(), any())).thenReturn(1L);
        when(articleDbService.getSourceId(anyString())).thenReturn(10L);

        UserRecommendationService.Candidate candidate = new UserRecommendationService.Candidate(100L, false, false, false, 0.90);
        when(recommendationService.findMatchingUsers(anyLong(), anyString())).thenReturn(List.of(candidate));

        kafkaNewsListener.processNewsStream(json);

        verify(notificationProducer, times(1)).send(eq(100L), eq("Test"), eq("http://test.com"), eq(0.90), eq("normal_ai"), eq(1L), eq("Tech"), eq("Technology"), eq(10L));
    }

    @Test
    void processNewsStream_DuplicateArticle_Aborts() throws Exception {
        String json = "{\"title\":\"Test\",\"link\":\"http://test.com\",\"source\":\"Tech\",\"summary\":\"Sum\"}";
        when(objectMapper.readTree(anyString())).thenReturn(new ObjectMapper().readTree(json));
        when(embeddingService.encode(anyString())).thenReturn(new float[]{0.1f});
        when(articleDbService.isDuplicate(anyString(), anyString(), anyString())).thenReturn(true);

        kafkaNewsListener.processNewsStream(json);

        verify(articleDbService, never()).insertArticle(any(), any(), any(), any(), any(), any());
    }

}