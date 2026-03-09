package md.botservice.events;

public record NewsNotificationEvent(
        Long userId,
        String postId,
        String title,
        String url,
        String sourceName,
        String topic
) {
}