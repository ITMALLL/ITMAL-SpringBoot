package com.itmal.auth.domain;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Builder(toBuilder = true)
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String email;
    private String password;
    private String nickname;
    private Role role;
    private String nativeLanguage;
    private String provider;
    private String providerId;
    private boolean emailVerified;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isSocialUser() {
        return provider != null;
    }
}
