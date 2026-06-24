package com.itmal.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(
        regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$",
        message = "비밀번호는 영문, 숫자, 특수문자(!@#$%^&*)를 포함한 8자 이상이어야 합니다."
    )
    private String password;

    @NotBlank
    private String passwordConfirm;

    @NotBlank
    @Size(min = 2, max = 20)
    private String nickname;

    @NotBlank
    private String nativeLanguage;
}
