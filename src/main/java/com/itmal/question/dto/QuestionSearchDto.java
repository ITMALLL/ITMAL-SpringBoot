package com.itmal.question.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionSearchDto {
    private String keyword;
    private Long languageId;
    private String category;
    private String target;
    private String sort = "latest";
    private int page = 1;
    private int size = 10;

    public int getOffset(){

        int safePage = Math.max(page, 1);
        return (safePage - 1) * size;

    }
}
