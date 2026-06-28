package com.itmal.auth.dto;

import com.itmal.auth.domain.User;
import lombok.Getter;

@Getter
public class UserResponse {

    private final Long userId;
    private final String nickname;

    private UserResponse(Long userId, String nickname) {
        this.userId = userId;
        this.nickname = nickname;
    }

    public static UserResponse from(User user) {
        return new UserResponse(user.getUserId(), user.getNickname());
    }
}
