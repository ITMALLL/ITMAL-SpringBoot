package com.itmal.answer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class AnswerLikeResponseDto {
    private boolean liked;
    private int likeCount;

    public AnswerLikeResponseDto(boolean liked, int likeCount) {
        this.liked = liked;
        this.likeCount = likeCount;
    }
}
