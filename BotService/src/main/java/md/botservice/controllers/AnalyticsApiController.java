package md.botservice.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import md.botservice.dto.*;
import md.botservice.service.AnalyticsEventService;
import md.botservice.service.SourceService;
import md.botservice.service.UserActivityService;
import md.botservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AnalyticsApiController {

    private final UserService userService;
    private final SourceService sourceService;
    private final UserActivityService userActivityService;
    private final AnalyticsEventService analyticsEventService;

    @GetMapping("/users/{userId}/recommendations")
    public ResponseEntity<List<SourceRecommendationDto>> getRecommendations(@PathVariable Long userId) {
        List<SourceRecommendationProjection> recommendations = userService.getRecommendationsForUser(userId);
        return ResponseEntity.ok(recommendations.stream()
                .map(SourceRecommendationDto::from)
                .toList()
        );
    }

    @GetMapping("/global-dashboard")
    public ResponseEntity<GlobalAnalyticsDashboardDto> getGlobalAnalytics() {
        GlobalAnalyticsDashboardDto stats = new GlobalAnalyticsDashboardDto(
                userService.getStrictModeAdoptionPercentage(),
                analyticsEventService.getAverageArticlesPerSession(),
                analyticsEventService.getAverageTopicEntropy(),
                userService.getTopReadAllSources(),
                analyticsEventService.getGlobalTopTopics(),
                analyticsEventService.geetSourceFeedbackStats(),
                userActivityService.getDailyActiveUsers(),
                sourceService.getTopSources()
        );
        log.info("Global stats: " + stats);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users/{userId}/dashboard")
    public ResponseEntity<UserAnalyticsDashboardDto> getUserDashboardStats(@PathVariable Long userId) {

        Long totalRead = analyticsEventService.getTotalArticlesRead(userId);
        Double ctr = analyticsEventService.getUserCtr(userId);
        Double avgSession = analyticsEventService.getUserAverageArticlesPerSession(userId);
        Double entropy = analyticsEventService.getUserTopicEntropy(userId);
        List<TopicReadProjection> topPersonalTopics = analyticsEventService.getTopReadTopics(userId);

        UserAnalyticsDashboardDto dashboard = new UserAnalyticsDashboardDto(
                totalRead,
                ctr,
                avgSession,
                entropy,
                topPersonalTopics
        );

        log.info("User stats: " + dashboard);
        return ResponseEntity.ok(dashboard);
    }
}
