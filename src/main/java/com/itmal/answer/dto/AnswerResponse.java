//답변 조회할 때 보내는 데이터
package com.itmal.answer.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AnswerResponse {

    private Long answerId; //답변아이디
    private String content; //답변내용
    private Long questionId; //어떤질문의 답변인지
    private Long userId; //작성자아이디
    private int likeCount; //좋아요 수
    private boolean isAccepted; //채택여부
    private LocalDateTime createdAt;

}
