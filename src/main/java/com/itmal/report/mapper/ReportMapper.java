package com.itmal.report.mapper;

import com.itmal.report.domain.Report;
import com.itmal.report.dto.ResponseReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportMapper {

    int insert(Report report);

    int updateStatus(@Param("reportId") Long reportId, @Param("status") String status);

    Report findById(Long reportId);

    int hideQuestion(Long questionId);

    List<ResponseReport> findPendingQuestions();

    List<ResponseReport> findPendingAnswers();

    List<ResponseReport> findPendingComments();

    Report findPendingByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);

}
