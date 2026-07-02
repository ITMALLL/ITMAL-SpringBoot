package com.itmal.question.dto;

import lombok.Getter;

@Getter
public enum Target {

    TUTOR("tutor", "튜터 전용"),
    COMMUNITY("community", "전체 사용자");


    private final String code;
    private final String label;

    Target(String code, String label) {
        this.code = code;
        this.label = label;
    }

    // 화면 표시용: 저장된 코드 → 한글 라벨 (모르는 코드는 그대로 반환해 안전)
    public static String labelOf(String code) {
        for (Target t : values()) {
            if (t.code.equals(code)) return t.label;
        }
        return code;
    }

}
