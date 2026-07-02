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
}
