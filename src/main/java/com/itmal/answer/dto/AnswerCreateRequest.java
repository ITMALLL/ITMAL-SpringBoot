//답변 작성할 때 받는 데이터
package com.itmal.answer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnswerCreateRequest {
    @NotNull
    private Long questionId;

    @NotBlank
    @Size(max = 2000)
    private String content;
}
