package com.itmal.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentRequestDto {

    @NotBlank(message = "댓글 내용을 입력해주세요.")
    @Size(max = 100, message = "댓글은 100자를 넘을 수 없습니다.")
    private String content;
}
