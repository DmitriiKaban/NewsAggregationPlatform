package md.botservice.dto;

import java.util.Map;

public record AnalyticsEventDto (Long userId, String type, String refId, String meta) {
}
