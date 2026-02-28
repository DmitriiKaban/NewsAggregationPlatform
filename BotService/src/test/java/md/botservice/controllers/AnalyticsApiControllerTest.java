package md.botservice.controllers;

import jakarta.ws.rs.core.MediaType;
import md.botservice.dto.DauProjection;
import md.botservice.dto.SourceRecommendationProjection;
import md.botservice.dto.TopSourceProjection;
import md.botservice.service.SourceService;
import md.botservice.service.UserActivityService;
import md.botservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsApiController.class)
class AnalyticsApiControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private SourceService sourceService;
    @MockitoBean
    private UserActivityService userActivityService;

    @Test
    @DisplayName("Test for the getRecommendations method, should return recommendations")
    void getRecommendationsTest() throws Exception {
        // Arrange
        long userId = 1L;
        SourceRecommendationProjection rec1 = mockRecommendation("Tech News", "https://t.me/tech", 15);
        SourceRecommendationProjection rec2 = mockRecommendation("SpaceX Updates", "https://t.me/spacex", 8);
        when(userService.getRecommendationsForUser(userId)).thenReturn(List.of(rec1, rec2));

        // Act & assert
        mockMvc.perform(get("/api/analytics/users/{userId}/recommendations", userId)
                        .content(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$.name").value("Tech News"))
                .andExpect(jsonPath("$.url").value("https://t.me/tech"))
                .andExpect(jsonPath("$.peerCount").value(15))
                .andExpect(jsonPath("$.name").value("SpaceX Updates"));
    }

    @Test
    @DisplayName("Test for getSystemInsights method, should return insights")
    void getSystemInsightsTest() throws Exception {
        // Arrange
        TopSourceProjection source1 = mockTopSource("durov", 123);
        TopSourceProjection source2 = mockTopSource("point", 124);

        DauProjection dau1 = mockDau(LocalDate.now().minusDays(1), 2);
        DauProjection dau2 = mockDau(LocalDate.now(), 3);

        when(sourceService.getTopSources()).thenReturn(List.of(source1, source2));
        when(userService.getInsights()).thenReturn(List.of(dau1, dau2));

        // Act & assert
        mockMvc.perform(get("/api/analytics/insights")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topSources.size()").value(2))
                .andExpect(jsonPath("$.dauStats.size()").value(2))

                .andExpect(jsonPath("$.topSources.name").value("durov"))
                .andExpect(jsonPath("$.topSources.subscriberCount").value(120))
                // Assert dauStats mapping
                .andExpect(jsonPath("$.dauStats.size()").value(2))
                .andExpect(jsonPath("$.dauStats.count").value(2))
                .andExpect(jsonPath("$.dauStats.count").value(3));
    }

    private SourceRecommendationProjection mockRecommendation(String name, String url, Integer peerCount) {
        SourceRecommendationProjection mock = mock(SourceRecommendationProjection.class);
        when(mock.getName()).thenReturn(name);
        when(mock.getUrl()).thenReturn(url);
        when(mock.getPeerCount()).thenReturn(peerCount);
        return mock;
    }

    private TopSourceProjection mockTopSource(String name, int subscriberCount) {
        TopSourceProjection mock = mock(TopSourceProjection.class);
        when(mock.getName()).thenReturn(name);
        when(mock.getSubscriberCount()).thenReturn(subscriberCount);
        return mock;
    }

    private DauProjection mockDau(LocalDate date, Integer count) {
        DauProjection mock = mock(DauProjection.class);
        when(mock.getDate()).thenReturn(date.toString());
        when(mock.getCount()).thenReturn(count);
        return mock;
    }
}