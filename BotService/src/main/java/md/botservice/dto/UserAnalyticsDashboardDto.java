package md.botservice.dto;

import java.util.List;

public record UserAnalyticsDashboardDto(
        Long totalArticlesRead,
        Double clickThroughRate,
        Double avgArticlesPerSession,
        Double avgTopicDiversityEntropy,
        List<TopicReadProjection> topReadTopics
) {}
