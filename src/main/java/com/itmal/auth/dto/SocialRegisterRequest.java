package com.itmal.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SocialRegisterRequest {

    @NotBlank
    @Size(min = 2, max = 20)
    private String nickname;

    @NotBlank
    private String nativeLanguage;

    @NotEmpty(message = "학습 언어를 1개 이상 선택해주세요.")
    private List<String> learningLanguages = new ArrayList<>();
}
