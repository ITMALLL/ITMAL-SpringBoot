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

    // 폼에서 받지 않고 서비스에서 세팅
    private Long userId;

    // insert 후 DB가 자동 생성한 PK 값이 세팅됨 (useGeneratedKeys)
    private Long answerId;
}
