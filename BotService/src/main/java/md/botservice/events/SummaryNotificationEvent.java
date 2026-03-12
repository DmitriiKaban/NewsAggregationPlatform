package md.botservice.events;

import java.util.List;

public record SummaryNotificationEvent(
        Long userId,
        String type,
        List<SummaryArticle> articles
) {
    public record SummaryArticle(
            String title,
            String sourceName,
            String url,
            String summary
    ) {}
}