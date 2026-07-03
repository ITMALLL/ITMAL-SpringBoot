package com.itmal.question.dto;

import lombok.Getter;
import lombok.Setter;

// 홈 화면 언어별 질문 현황 카드용 DTO
@Getter
@Setter
public class LanguageStatDto {
    private String languageName;
    private String languageCode;
    private long questionCount;
    private int percent;
}
