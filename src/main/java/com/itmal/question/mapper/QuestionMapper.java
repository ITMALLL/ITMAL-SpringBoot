package com.itmal.question.mapper;

import com.itmal.question.dto.LanguageDto;
import com.itmal.question.dto.LanguageStatDto;
import com.itmal.question.dto.QuestionAttachmentDto;
import com.itmal.question.dto.QuestionDto;
import com.itmal.question.dto.QuestionSearchDto;
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

    // 번역 캐시 갱신 (Papago 결과 저장)
    void updateTranslation(@Param("questionId") Long questionId,
                           @Param("translatedContent") String translatedContent,
                           @Param("translatedTarget") String translatedTarget);

    // languageId → languageCode (번역 대상 언어코드 조회)
    String findLanguageCodeById(Long languageId);

    void deleteAttachment(Long id); // 스케쥴러용

    int softDeleteAttachment(@Param("id") Long id, @Param("questionId") Long questionId);

    void softDeleteAttachmentsByQuestionId(Long questionId);

    List<QuestionAttachmentDto> findAttachmentsToPurge();

    List<QuestionDto> findRelatedQuestions(@Param("questionId") Long questionId,
                                           @Param("languageId") Long languageId,
                                           @Param("category") String category);

    List<QuestionDto> findQuestions(QuestionSearchDto search);
    int countQuestions(QuestionSearchDto search);

    // ── 홈 화면 통계 ─────────────────────────────────────────
    long countAllQuestions();                       // 등록된 질문 수
    long countAllAnswers();                          // 답변 수
    long countActiveUsers();                         // 활성 유저(탈퇴하지 않은 회원) 수
    long countLanguages();                           // 지원 언어 수
    List<QuestionDto> findRecentQuestions();         // 최근 질문(최신순 5건)
    List<LanguageStatDto> countQuestionsByLanguage();// 언어별 질문 현황

}