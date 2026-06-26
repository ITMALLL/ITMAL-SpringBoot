package com.itmal.answer.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Answer {
    private Long answerId;
    private Long questionId;
    private Long userId;
    private String content;
    private int likeCount;
    private boolean isAccepted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
