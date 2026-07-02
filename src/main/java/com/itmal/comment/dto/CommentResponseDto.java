package com.itmal.comment.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CommentResponseDto {
    private Long commentId;
    private String content;
    private String nickname;  // user 테이블에서 가져옴
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
