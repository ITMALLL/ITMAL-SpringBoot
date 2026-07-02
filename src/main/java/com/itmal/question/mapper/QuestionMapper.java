package com.itmal.question.mapper;

import com.itmal.question.dto.LanguageDto;
import com.itmal.question.dto.QuestionAttachmentDto;
import com.itmal.question.dto.QuestionDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionMapper {

    List<QuestionDto> findByUserId(Long userId);

    List<QuestionDto> findAllQuestions();

    List<LanguageDto> findAllLanguages();

    QuestionAttachmentDto findAttachmentById(Long id);

    List<QuestionAttachmentDto> findAttachmentsByQuestionId(Long questionId);

    void insertQuestion(QuestionDto questionDto);

    void insertAttachment(QuestionAttachmentDto questionAttachmentDto);

    QuestionDto findQuestionDetailById(Long id);

    int countQuestionsByUserId(Long userId);

    void increaseViewCount(Long id);

    int softDeleteQuestion(@Param("questionId") Long questionId, @Param("userId") Long userId);

    int updateQuestion(@Param("question") QuestionDto questionDto, @Param("userId") Long userId);

    void deleteAttachment(Long id); // 스케쥴러용

    int softDeleteAttachment(@Param("id") Long id, @Param("questionId") Long questionId);

    void softDeleteAttachmentsByQuestionId(Long questionId);

    List<QuestionAttachmentDto> findAttachmentsToPurge();

    List<QuestionDto> findRelatedQuestions(@Param("questionId") Long questionId,
                                           @Param("languageId") Long languageId,
                                           @Param("category") String category);

}