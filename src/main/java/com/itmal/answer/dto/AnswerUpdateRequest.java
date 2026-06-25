//답변 수정할 때 받는 데이터
package com.itmal.answer.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnswerUpdateRequest {
    private Long questionId;
    private String content;

}
