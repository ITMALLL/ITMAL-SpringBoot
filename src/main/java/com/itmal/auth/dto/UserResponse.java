package com.itmal.auth.dto;

import com.itmal.auth.domain.User;
import lombok.Getter;

@Getter
public class UserResponse {

    private final Long userId;
    private final String nickname;
    private final String nativeLanguage;

    private UserResponse(Long userId, String nickname, String nativeLanguage) {
        this.userId = userId;
        this.nickname = nickname;
        this.nativeLanguage = nativeLanguage;
    }

    public static UserResponse from(User user) {
        return new UserResponse(user.getUserId(), user.getNickname(), user.getNativeLanguage());
    }
}
