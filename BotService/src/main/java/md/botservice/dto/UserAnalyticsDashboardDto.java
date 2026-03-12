package md.botservice.dto;

public record UserAnalyticsDashboardDto(
        Long totalArticlesRead,
        Double clickThroughRate,
        Double avgArticlesPerSession,
        Double avgTopicDiversityEntropy
) {}
