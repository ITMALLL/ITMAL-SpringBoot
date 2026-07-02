package com.itmal.question.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class QuestionDto {
    private Long questionId;
    private String title;
    private String content;
    private Long viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String category;
    private LocalDateTime deletedAt;
    private Long userId;
    private Long languageId;
    private String target;
    private String nickname;
    private String languageName;
    private String languageCode;
    private LocalDateTime authorJoinedAt;
    private String nativeLanguage;
    private String role;
    private String status;         // 답변 상태: PENDING / ANSWERED / ADOPTED
    private String translatedContent;  // Papago 번역본(캐시). null=미번역/실패, ""=원문과 동일 언어
    private String translatedTarget;   // 번역 대상 언어코드(캐시 유효성 판정용)
    private long answerCount;          // 홈 "최근 질문" 카드에 표시할 답변 수
}
