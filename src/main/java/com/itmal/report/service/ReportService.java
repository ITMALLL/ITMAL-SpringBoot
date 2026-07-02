package com.itmal.report.service;

import com.itmal.answer.mapper.AnswerMapper;
import com.itmal.comment.mapper.CommentMapper;
import com.itmal.global.exception.ApiException;
import com.itmal.global.exception.ErrorCode;
import com.itmal.report.domain.Report;
import com.itmal.report.dto.CreateReportDto;
import com.itmal.report.dto.ResponseReport;
import com.itmal.report.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;
    private final AnswerMapper answerMapper;
    private final CommentMapper commentMapper;

    public void createReport(CreateReportDto createReportDto, Long userId){
        Report existing = reportMapper.findPendingByTarget(
                createReportDto.getTargetType(), createReportDto.getTargetId());
        if(existing != null){
            throw new ApiException(ErrorCode.DUPLICATE_REPORT);
        }

        Report report = new Report();
        report.setTargetType(createReportDto.getTargetType());
        report.setTargetId(createReportDto.getTargetId());
        report.setReason(createReportDto.getReason());
        report.setStatus("PENDING");
        report.setUserId(userId);

        reportMapper.insert(report);
    }

    @Transactional
    public void updateStatus(Long reportId, String status){
        int updated = reportMapper.updateStatus(reportId, status);
        if(updated == 0){
            throw new ApiException(ErrorCode.REPORT_NOT_FOUND);
        }

        if ("APPROVED".equals(status)) {
            Report report = reportMapper.findById(reportId);
            switch (report.getTargetType()) {
                case "QUESTION" -> reportMapper.hideQuestion(report.getTargetId());
                case "ANSWER" -> answerMapper.deleteAnswer(report.getTargetId());
                case "COMMENT" -> commentMapper.delete(report.getTargetId());
            }
        }
    }

    public List<ResponseReport> findPendingQuestions(){
        return reportMapper.findPendingQuestions();
    }

    public List<ResponseReport> findPendingAnswers(){
        return reportMapper.findPendingAnswers();
    }

    public List<ResponseReport> findPendingComments(){
        return reportMapper.findPendingComments();
    }

    public Map<String, Object> countPendingByType(){
        // 타입 3개 다 0으로 미리 채워둠 - 신고가 0건인 타입도 프론트에서 키가 없어서 에러나는 일이 없게
        Map<String, Object> result = new HashMap<>();
        result.put("QUESTION", 0L);
        result.put("ANSWER", 0L);
        result.put("COMMENT", 0L);

        List<Map<String, Object>> rows = reportMapper.countPendingByType();
        long total = 0;

        for (Map<String, Object> row : rows) {
            String type = (String) row.get("targetType");
            Long count = (Long) row.get("count");
            result.put(type, count);
            total += count;
        }

        result.put("total", total);
        return result;
    }

}
