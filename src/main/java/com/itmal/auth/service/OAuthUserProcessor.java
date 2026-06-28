package com.itmal.auth.service;

import com.itmal.auth.domain.Role;
import com.itmal.auth.domain.User;
import com.itmal.auth.dto.OAuthAttributes;
import com.itmal.auth.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthUserProcessor {

    private final UserMapper userMapper;

    public User getOrSaveUser(OAuthAttributes attributes) {
        // 1. provider/providerId로 기존 OAuth 계정 조회
        return userMapper.findByProviderAndProviderId(attributes.getProvider(), attributes.getProviderId())
                .orElseGet(() -> findOrCreateByEmail(attributes));
    }

    private User findOrCreateByEmail(OAuthAttributes attributes) {
        return userMapper.findByEmail(attributes.getEmail())
                .map(existingUser -> linkOAuthToExisting(existingUser, attributes))
                .orElseGet(() -> createNewUser(attributes));
    }

    private User linkOAuthToExisting(User existingUser, OAuthAttributes attributes) {
        // 2. 동일 이메일 일반 가입 계정 → OAuth 연결
        userMapper.updateProvider(existingUser.getUserId(), attributes.getProvider(), attributes.getProviderId());
        return existingUser.toBuilder()
                .provider(attributes.getProvider())
                .providerId(attributes.getProviderId())
                .build();
    }

    private User createNewUser(OAuthAttributes attributes) {
        // 3. 신규 유저 생성
        String nickname = resolveNickname(attributes.getNickname());
        User newUser = User.builder()
                .email(attributes.getEmail())
                .nickname(nickname)
                .provider(attributes.getProvider())
                .providerId(attributes.getProviderId())
                .role(Role.ROLE_USER)
                .emailVerified(true)
                .build();
        userMapper.insert(newUser); // useGeneratedKeys → newUser.getUserId() 자동 세팅
        return newUser;
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
