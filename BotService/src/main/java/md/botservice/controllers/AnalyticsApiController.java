package md.botservice.controllers;

import lombok.RequiredArgsConstructor;
import md.botservice.dto.DauProjection;
import md.botservice.dto.InsightsDto;
import md.botservice.dto.SourceRecommendationProjection;
import md.botservice.dto.TopSourceProjection;
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
public class AnalyticsApiController {

    private final UserService userService;
    private final SourceService sourceService;
    private final UserActivityService userActivityService;

    @GetMapping("/users/{userId}/recommendations")
    public ResponseEntity<List<SourceRecommendationProjection>> getRecommendations(@PathVariable Long userId) {
        List<SourceRecommendationProjection> recommendations = userService.getRecommendationsForUser(userId);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/insights")
    public ResponseEntity<InsightsDto> getSystemInsights() {

        List<DauProjection> dauList = userActivityService.getDailyActiveUsers();
        List<TopSourceProjection> topSources = sourceService.getTopSources();

        return ResponseEntity.ok(InsightsDto.of(topSources, dauList));
    }
}
