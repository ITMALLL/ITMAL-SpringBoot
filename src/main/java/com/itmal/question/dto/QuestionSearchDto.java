package com.itmal.question.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionSearchDto {
    private static final int MAX_SIZE = 100;

    private String keyword;
    private Long languageId;
    private String category;
    private String target;
    private String sort = "latest";
    private int page = 1;
    private int size = 10;

    // page는 1 이상으로 보정 (음수/0 요청 방어)
    public int getPage() {
        return Math.max(page, 1);
    }

    // size는 1 이상 MAX_SIZE 이하로 보정 (음수 LIMIT/과도한 조회 방어)
    public int getSize() {
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }

    public int getOffset(){
        return (getPage() - 1) * getSize();
    }
}
