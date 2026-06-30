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

    List<QuestionDto> findRelatedQuestions(@Param("questionId") Long questionId,
                                           @Param("languageId") Long languageId,
                                           @Param("category") String category);

}