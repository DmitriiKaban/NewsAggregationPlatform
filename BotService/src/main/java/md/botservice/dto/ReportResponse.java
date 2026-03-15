package md.botservice.dto;

import java.time.LocalDateTime;

public record ReportResponse(
        Long id,
        Long articleId,
        Long sourceId,
        SourceInfo source,
        ReporterInfo reporter,
        String reason,
        String status,
        LocalDateTime reportedAt
) {
    public record SourceInfo(Long id, String name, String url) {}
    public record ReporterInfo(Long id, String username) {}
}