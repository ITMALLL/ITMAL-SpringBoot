//답변 조회할 때 보내는 데이터
package com.itmal.answer.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AnswerResponse {

    private Long answerId;
    private String content;
    private Long questionId;
    private Long userId;
    private String nickname;
    private int likeCount;
    private boolean isAccepted;
    private LocalDateTime createdAt;

}
