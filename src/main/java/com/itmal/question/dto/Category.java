package com.itmal.question.dto;

import lombok.Getter;

@Getter
public enum Category {
    GRAMMAR("grammar", "문법"),
    WORD("word", "단어"),
    EXPRESSION("expression", "표현"),
    TRANSLATION("translation", "번역"),
    WRITING("writing", "작문"),
    PRONUNCIATION("pronunciation", "발음"),
    CULTURE("culture", "문화"),
    ETC("etc", "기타");

    private final String code;
    private final String label;

    Category(String code, String label) {
        this.code = code;
        this.label = label;
    }

    // 화면 표시용: 저장된 코드 → 한글 라벨 (모르는 코드는 그대로 반환해 안전)
    public static String labelOf(String code) {
        for (Category c : values()) {
            if (c.code.equals(code)) return c.label;
        }
        return code;
    }
}
