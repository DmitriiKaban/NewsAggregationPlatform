package md.botservice.dto;

import java.util.List;

public record GlobalAnalyticsDashboardDto(
        Double strictModeAdoptionPercent,
        Double avgArticlesPerSession,
        Double avgTopicDiversityEntropy,
        List<SourceAdoptionProjection> topReadAllSources,
        List<TopicReadProjection> topGlobalTopics,
        List<SourceFeedbackProjection> sourceFeedback,
        List<DauProjection> dauStats,
        List<TopSourceProjection> topSources
) {}
