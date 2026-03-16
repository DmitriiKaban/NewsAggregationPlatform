package md.botservice.controllers;

import lombok.RequiredArgsConstructor;
import md.botservice.dto.ReportRequest;
import md.botservice.dto.ReportResponse;
import md.botservice.models.ReportStatus;
import md.botservice.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportApiController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<?> submitReport(@RequestBody ReportRequest request) {
        try {
            reportService.submitReport(request);
            return ResponseEntity.ok(Map.of("message", "Report submitted successfully"));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<ReportResponse>> getAllReports(@RequestParam(required = false) Long moderatorId) {
        List<ReportResponse> reports = reportService.getAllReports(moderatorId);
        return ResponseEntity.ok(reports);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> updateReportStatus(
            @PathVariable Long id,
            @RequestParam ReportStatus status,
            @RequestParam Long moderatorId) {
        reportService.updateReportStatus(id, status, moderatorId);
        return ResponseEntity.ok(Map.of("message", "Status updated successfully"));
    }

}