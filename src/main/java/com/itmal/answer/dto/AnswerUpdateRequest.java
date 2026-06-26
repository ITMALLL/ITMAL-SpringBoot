//답변 수정할 때 받는 데이터
package com.itmal.answer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnswerUpdateRequest {
    @NotBlank
    @Size(max = 2000)
    private String content;
}
