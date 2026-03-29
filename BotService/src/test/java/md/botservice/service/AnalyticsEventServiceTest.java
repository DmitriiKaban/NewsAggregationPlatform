package md.botservice.service;

import md.botservice.dto.SourceFeedbackProjection;
import md.botservice.dto.TopicReadProjection;
import md.botservice.repository.AnalyticsEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsEventServiceTest {

    @Mock
    private AnalyticsEventRepository analyticsEventRepository;

    @InjectMocks
    private AnalyticsEventService analyticsEventService;

    @Test
    void getAverageArticlesPerSession() {
        when(analyticsEventRepository.getGlobalAverageArticlesPerSession()).thenReturn(4.5);
        assertEquals(4.5, analyticsEventService.getAverageArticlesPerSession());
        verify(analyticsEventRepository).getGlobalAverageArticlesPerSession();
    }

    @Test
    void getAverageTopicEntropy() {
        when(analyticsEventRepository.getAverageTopicEntropy()).thenReturn(1.2);
        assertEquals(1.2, analyticsEventService.getAverageTopicEntropy());
        verify(analyticsEventRepository).getAverageTopicEntropy();
    }

    @Test
    void getGlobalTopTopics() {
        List<TopicReadProjection> mockTopics = Collections.emptyList();
        when(analyticsEventRepository.getGlobalTopTopics()).thenReturn(mockTopics);
        assertEquals(mockTopics, analyticsEventService.getGlobalTopTopics());
        verify(analyticsEventRepository).getGlobalTopTopics();
    }

    @Test
    void getSourceFeedbackStats() {
        List<SourceFeedbackProjection> mockStats = Collections.emptyList();
        when(analyticsEventRepository.getSourceFeedbackStats()).thenReturn(mockStats);
        assertEquals(mockStats, analyticsEventService.geetSourceFeedbackStats());
        verify(analyticsEventRepository).getSourceFeedbackStats();
    }

    @Test
    void getTotalArticlesRead() {
        when(analyticsEventRepository.getTotalArticlesRead(1L)).thenReturn(100L);
        assertEquals(100L, analyticsEventService.getTotalArticlesRead(1L));
        verify(analyticsEventRepository).getTotalArticlesRead(1L);
    }

    @Test
    void getUserCtr() {
        when(analyticsEventRepository.getUserCtr(1L)).thenReturn(0.85);
        assertEquals(0.85, analyticsEventService.getUserCtr(1L));
        verify(analyticsEventRepository).getUserCtr(1L);
    }

    @Test
    void getUserAverageArticlesPerSession() {
        when(analyticsEventRepository.getUserAverageArticlesPerSession(1L)).thenReturn(3.2);
        assertEquals(3.2, analyticsEventService.getUserAverageArticlesPerSession(1L));
        verify(analyticsEventRepository).getUserAverageArticlesPerSession(1L);
    }

    @Test
    void getUserTopicEntropy() {
        when(analyticsEventRepository.getUserTopicEntropy(1L)).thenReturn(0.9);
        assertEquals(0.9, analyticsEventService.getUserTopicEntropy(1L));
        verify(analyticsEventRepository).getUserTopicEntropy(1L);
    }

    @Test
    void getTopReadTopics() {
        List<TopicReadProjection> mockTopics = Collections.emptyList();
        when(analyticsEventRepository.getTopReadTopics(1L)).thenReturn(mockTopics);
        assertEquals(mockTopics, analyticsEventService.getTopReadTopics(1L));
        verify(analyticsEventRepository).getTopReadTopics(1L);
    }

}