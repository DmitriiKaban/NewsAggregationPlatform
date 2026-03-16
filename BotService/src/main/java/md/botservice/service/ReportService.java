package md.botservice.service;

import lombok.RequiredArgsConstructor;
import md.botservice.dto.ReportRequest;
import md.botservice.dto.ReportResponse;
import md.botservice.models.Report;
import md.botservice.models.ReportStatus;
import md.botservice.models.User;
import md.botservice.models.UserRole;
import md.botservice.repository.ArticleRepository;
import md.botservice.repository.ReportRepository;
import md.botservice.repository.SourceRepository;
import md.botservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final SourceRepository sourceRepository;
    private final ArticleRepository articleRepository;

    @Transactional
    public Report submitReport(ReportRequest request) {
        if (request.getReporterId() == null) {
            throw new IllegalArgumentException("Reporter ID is required");
        }

        if (request.getArticleId() != null) {
            if (reportRepository.existsByReporterIdAndArticleId(request.getReporterId(), request.getArticleId())) {
                throw new IllegalStateException("You have already reported this article.");
            }
        } else if (request.getSourceId() != null) {
            if (reportRepository.existsByReporterIdAndSourceIdAndArticleIdIsNull(request.getReporterId(), request.getSourceId())) {
                throw new IllegalStateException("You have already reported this source.");
            }
        }

        User reporter = userRepository.findById(request.getReporterId())
                .orElseThrow(() -> new IllegalArgumentException("Reporter not found"));

        Report report = new Report();
        report.setReporter(reporter);
        report.setReason(request.getReason());
        report.setStatus(ReportStatus.PENDING);
        report.setArticleId(request.getArticleId());
        report.setSourceId(request.getSourceId());

        if (request.getReportedUserId() != null) {
            userRepository.findById(request.getReportedUserId()).ifPresent(report::setReportedUser);
        }

        return reportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getAllReports(Long moderatorId) {
        verifyModeratorAccess(moderatorId);

        List<Report> reports = reportRepository.findAllByOrderByReportedAtDesc();

        return reports.stream().map(r -> {
            ReportResponse.SourceInfo sourceInfo = null;

            if (r.getSource() != null) {
                sourceInfo = new ReportResponse.SourceInfo(r.getSource().getId(), r.getSource().getName(), r.getSource().getUrl());
            } else if (r.getSourceId() != null) {
                sourceInfo = sourceRepository.findById(r.getSourceId())
                        .map(s -> new ReportResponse.SourceInfo(s.getId(), s.getName(), s.getUrl()))
                        .orElse(null);
            }

            ReportResponse.ArticleInfo articleInfo = null;
            if (r.getArticleId() != null) {
                articleInfo = articleRepository.findById(r.getArticleId())
                        .map(a -> new ReportResponse.ArticleInfo(a.getId(), a.getTitle(), a.getUrl()))
                        .orElse(null);
            }

            ReportResponse.ReporterInfo reporterInfo = null;
            if (r.getReporter() != null) {
                reporterInfo = new ReportResponse.ReporterInfo(r.getReporter().getId(), r.getReporter().getUsername());
            }

            return new ReportResponse(
                    r.getId(),
                    r.getArticleId(),
                    r.getSourceId(),
                    sourceInfo,
                    articleInfo,
                    reporterInfo,
                    r.getReason() != null ? r.getReason().name() : "UNKNOWN",
                    r.getStatus() != null ? r.getStatus().name() : "PENDING",
                    r.getReportedAt()
            );
        }).toList();
    }

    @Transactional
    public Report updateReportStatus(Long reportId, ReportStatus status, Long moderatorId) {
        verifyModeratorAccess(moderatorId);

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        report.setStatus(status);
        return reportRepository.save(report);
    }

    private void verifyModeratorAccess(Long userId) {
        if (userId == null) {
            throw new SecurityException("Moderator ID is required");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SecurityException("User not found"));

        if (user.getRole() != UserRole.MODERATOR && user.getRole() != UserRole.ADMIN) {
            throw new SecurityException("Forbidden: Insufficient privileges");
        }
    }

}