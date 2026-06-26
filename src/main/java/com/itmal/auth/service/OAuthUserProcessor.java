package com.itmal.auth.service;

import com.itmal.auth.domain.Role;
import com.itmal.auth.domain.User;
import com.itmal.auth.dto.OAuthAttributes;
import com.itmal.auth.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuthUserProcessor {

    private final UserMapper userMapper;

    public User getOrSaveUser(OAuthAttributes attributes) {
        return userMapper.findByProviderAndProviderId(attributes.getProvider(), attributes.getProviderId())
                .orElseGet(() -> {
                    String nickname = resolveNickname(attributes.getNickname());
                    User newUser = User.builder()
                            .email(attributes.getEmail())
                            .nickname(nickname)
                            .provider(attributes.getProvider())
                            .providerId(attributes.getProviderId())
                            .role(Role.ROLE_USER)
                            .emailVerified(true)
                            .build();
                    userMapper.insert(newUser);
                    return userMapper.findByProviderAndProviderId(attributes.getProvider(), attributes.getProviderId())
                            .orElseThrow();
                });
    }

    private String resolveNickname(String base) {
        if (!userMapper.existsByNickname(base)) {
            return base;
        }
        // TODO: 닉네임 설정 페이지로 개선 예정
        for (int i = 0; i < 5; i++) {
            String candidate = base + "_" + UUID.randomUUID().toString().substring(0, 4);
            if (!userMapper.existsByNickname(candidate)) {
                return candidate;
            }
        }
        return base + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
