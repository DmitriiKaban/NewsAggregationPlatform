package md.botservice.service;

import lombok.RequiredArgsConstructor;
import md.botservice.dto.SourceFeedbackProjection;
import md.botservice.dto.TopicReadProjection;
import md.botservice.repository.AnalyticsEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsEventService {
    private final AnalyticsEventRepository analyticsEventRepository;

    public Double getAverageArticlesPerSession() {
        return analyticsEventRepository.getGlobalAverageArticlesPerSession();
    }

    public Double getAverageTopicEntropy() {
        return analyticsEventRepository.getAverageTopicEntropy();
    }

    public List<TopicReadProjection> getGlobalTopTopics() {
        return analyticsEventRepository.getGlobalTopTopics();
    }

    public List<SourceFeedbackProjection> geetSourceFeedbackStats() {
        return analyticsEventRepository.getSourceFeedbackStats();
    }

    public Long getTotalArticlesRead(Long userId) {
        return analyticsEventRepository.getTotalArticlesRead(userId);
    }

    public Double getUserCtr(Long userId) {
        return analyticsEventRepository.getUserCtr(userId);
    }

    public Double getUserAverageArticlesPerSession(Long userId) {
        return analyticsEventRepository.getUserAverageArticlesPerSession(userId);
    }

    public Double getUserTopicEntropy(Long userId) {
        return analyticsEventRepository.getUserTopicEntropy(userId);
    }

    public List<TopicReadProjection> getTopReadTopics(Long userId) {
        return analyticsEventRepository.getTopReadTopics(userId);
    }
}
