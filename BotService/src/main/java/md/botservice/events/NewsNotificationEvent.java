package md.botservice.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NewsNotificationEvent(
        Long userId,
        String postId,
        String title,
        String url,
        String sourceName,
        String topic
) {
}