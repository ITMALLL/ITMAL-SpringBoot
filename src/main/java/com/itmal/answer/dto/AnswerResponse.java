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
    private boolean liked;          // 현재 로그인 유저가 이 답변에 좋아요를 눌렀는지 (새로고침 시 상태 유지용)
    private boolean isAccepted;
    private LocalDateTime createdAt;

}
