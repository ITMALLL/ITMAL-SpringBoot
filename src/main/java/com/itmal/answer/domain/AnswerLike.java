package com.itmal.answer.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnswerLike {
    private Long id;
    private Long userId;
    private Long answerId;
}
