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

import java.util.List;

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
            return; //이미 신고가 된 글일 경우 db저장은 안하지만 사용자에게는 "신고되었습니다" 로 응답
        }

        Report report = new Report();
        report.setTargetType(createReportDto.getTargetType());
        report.setTargetId(createReportDto.getTargetId());
        report.setReason(createReportDto.getReason());
        report.setStatus("PENDING");
        report.setUserId(userId);

        reportMapper.insert(report);
    }

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

}
