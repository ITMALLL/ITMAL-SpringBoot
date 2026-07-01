package com.itmal.report.controller;

import com.itmal.auth.domain.CustomUserDetails;
import com.itmal.report.dto.CreateReportDto;
import com.itmal.report.dto.ResponseReport;
import com.itmal.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<String> createReport(
            @RequestBody CreateReportDto createReportDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        reportService.createReport(createReportDto, userDetails.getUserId());
        return ResponseEntity.ok("신고되었습니다.");
    }

    // 관리자 - 승인/거절
    @PutMapping("/{reportId}/approve")
    public ResponseEntity<String> approve(@PathVariable Long reportId) {
        reportService.updateStatus(reportId, "APPROVED");
        return ResponseEntity.ok("승인 처리되었습니다.");
    }

    @PutMapping("/{reportId}/reject")
    public ResponseEntity<String> reject(@PathVariable Long reportId) {
        reportService.updateStatus(reportId, "REJECTED");
        return ResponseEntity.ok("거절 처리되었습니다.");
    }

    // 관리자 - 탭별 대기 목록
    @GetMapping("/pending/questions")
    public ResponseEntity<List<ResponseReport>> getPendingQuestions() {
        return ResponseEntity.ok(reportService.findPendingQuestions());
    }

    @GetMapping("/pending/answers")
    public ResponseEntity<List<ResponseReport>> getPendingAnswers() {
        return ResponseEntity.ok(reportService.findPendingAnswers());
    }

    @GetMapping("/pending/comments")
    public ResponseEntity<List<ResponseReport>> getPendingComments() {
        return ResponseEntity.ok(reportService.findPendingComments());
    }

    @GetMapping("/pending/count")
    public ResponseEntity<Map<String, Object>> getPendingCount() {
        return ResponseEntity.ok(reportService.countPendingByType());
    }

}
