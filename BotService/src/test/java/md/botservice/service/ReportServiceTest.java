package md.botservice.service;

import md.botservice.dto.ReportRequest;
import md.botservice.dto.ReportResponse;
import md.botservice.models.*;
import md.botservice.repository.ArticleRepository;
import md.botservice.repository.ReportRepository;
import md.botservice.repository.SourceRepository;
import md.botservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private ReportRepository reportRepository;
    @Mock private UserRepository userRepository;
    @Mock private SourceRepository sourceRepository;
    @Mock private ArticleRepository articleRepository;

    @InjectMocks
    private ReportService reportService;

    private User reporterUser;
    private User moderatorUser;

    @BeforeEach
    void setUp() {
        reporterUser = new User();
        reporterUser.setId(1L);
        reporterUser.setUsername("reporter");
        reporterUser.setRole(UserRole.USER);

        moderatorUser = new User();
        moderatorUser.setId(2L);
        moderatorUser.setRole(UserRole.MODERATOR);
    }

    private ReportRequest createReportRequest(Long reporterId, Long articleId, Long sourceId, Long reportedUserId, ReportReason reason) {
        ReportRequest request = new ReportRequest();
        request.setReporterId(reporterId);
        request.setArticleId(articleId);
        request.setSourceId(sourceId);
        request.setReportedUserId(reportedUserId);
        request.setReason(reason);
        return request;
    }

    @Test
    void submitReport_MissingReporterId_ThrowsException() {
        ReportRequest request = createReportRequest(null, 10L, null, null, ReportReason.SPAM);
        assertThrows(IllegalArgumentException.class, () -> reportService.submitReport(request));
    }

    @Test
    void submitReport_DuplicateArticleReport_ThrowsException() {
        ReportRequest request = createReportRequest(1L, 10L, null, null, ReportReason.SPAM);
        when(reportRepository.existsByReporterIdAndArticleId(1L, 10L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> reportService.submitReport(request));
    }

    @Test
    void submitReport_DuplicateSourceReport_ThrowsException() {
        ReportRequest request = createReportRequest(1L, null, 20L, null, ReportReason.SPAM);
        when(reportRepository.existsByReporterIdAndSourceIdAndArticleIdIsNull(1L, 20L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> reportService.submitReport(request));
    }

    @Test
    void submitReport_ReporterNotFound_ThrowsException() {
        ReportRequest request = createReportRequest(99L, 10L, null, null, ReportReason.SPAM);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> reportService.submitReport(request));
    }

    @Test
    void submitReport_Success_SavesReport() {
        ReportRequest request = createReportRequest(1L, 10L, 20L, 30L, ReportReason.HATE_SPEECH);
        when(reportRepository.existsByReporterIdAndArticleId(1L, 10L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(reporterUser));

        User reportedUser = new User();
        reportedUser.setId(30L);
        when(userRepository.findById(30L)).thenReturn(Optional.of(reportedUser));

        Report mockSavedReport = new Report();
        when(reportRepository.save(any(Report.class))).thenReturn(mockSavedReport);

        Report result = reportService.submitReport(request);

        assertNotNull(result);
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    void getAllReports_Unauthorized_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(reporterUser));

        assertThrows(SecurityException.class, () -> reportService.getAllReports(1L));
    }

    @Test
    void getAllReports_Success_MapsCorrectly() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(moderatorUser));

        Report report = new Report();
        report.setId(100L);
        report.setArticleId(10L);
        report.setSourceId(20L);
        report.setReporter(reporterUser);
        report.setReason(ReportReason.MISLEADING);
        report.setStatus(ReportStatus.PENDING);

        Article article = new Article();
        article.setId(10L);
        article.setTitle("Test Title");

        Source source = new Source();
        source.setId(20L);
        source.setName("Test Source");

        when(reportRepository.findAllByOrderByReportedAtDesc()).thenReturn(List.of(report));
        when(articleRepository.findById(10L)).thenReturn(Optional.of(article));
        when(sourceRepository.findById(20L)).thenReturn(Optional.of(source));

        List<ReportResponse> responses = reportService.getAllReports(2L);

        assertEquals(1, responses.size());
        ReportResponse response = responses.getFirst();
        assertEquals(100L, response.id());
        assertEquals("MISLEADING", response.reason());

        assertEquals("Test Title", response.article().title());
        assertEquals("Test Source", response.source().name());
        assertEquals("reporter", response.reporter().username());
    }

    @Test
    void updateReportStatus_Success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(moderatorUser));
        Report report = new Report();
        report.setStatus(ReportStatus.PENDING);
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(reportRepository.save(any(Report.class))).thenReturn(report);

        Report result = reportService.updateReportStatus(100L, ReportStatus.RESOLVED, 2L);

        assertEquals(ReportStatus.RESOLVED, result.getStatus());
        verify(reportRepository).save(report);
    }

}