package md.faf223.airecommendationsservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicClassifierServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private TopicClassifierService topicClassifierService;

    @BeforeEach
    void setUp() throws Exception {
        when(embeddingService.encode(anyString())).thenReturn(new float[]{1.0f, 0.0f, 0.0f});
        topicClassifierService.initializeTopicVectors();
    }

    @Test
    void predictTopic_NullOrEmptyVector_ReturnsGeneralNews() {
        assertEquals("General News", topicClassifierService.predictTopic(null));
        assertEquals("General News", topicClassifierService.predictTopic(new float[]{}));
    }

    @Test
    void predictTopic_HighSimilarity_ReturnsMatchingTopic() {
        String topic = topicClassifierService.predictTopic(new float[]{1.0f, 0.0f, 0.0f});
        assertNotEquals("General News", topic);
    }

    @Test
    void predictTopic_LowSimilarity_ReturnsGeneralNews() {
        String topic = topicClassifierService.predictTopic(new float[]{0.0f, 1.0f, 0.0f});
        assertEquals("General News", topic);
    }

    @Test
    void toVectorString_FormatsCorrectly() {
        String result = topicClassifierService.toVectorString(new float[]{0.1f, 0.2f});
        assertEquals("[0.1, 0.2]", result);
    }

}