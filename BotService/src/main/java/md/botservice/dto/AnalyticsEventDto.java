package md.botservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnalyticsEventDto (Long userId, String type, String refId, String meta) {
}
