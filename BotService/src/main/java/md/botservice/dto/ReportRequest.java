package md.botservice.dto;

import lombok.Data;
import md.botservice.models.ReportReason;

@Data
public class ReportRequest {
    private Long articleId;
    private Long sourceId;
    private Long reporterId;
    private Long reportedUserId;
    private ReportReason reason;
}