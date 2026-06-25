package com.itmal.papago.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PapagoRequestDto {
    @NotBlank(message = "source 언어는 필수입니다")
    private String source;

    @NotBlank(message = "target 언어는 필수입니다")
    private String target;

    @NotBlank(message = "번역할 텍스트는 필수입니다")
    private String text;
}
