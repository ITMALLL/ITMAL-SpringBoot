package com.itmal.auth.service;

import com.itmal.auth.domain.CustomUserDetails;
import com.itmal.auth.domain.Role;
import com.itmal.auth.domain.User;
import com.itmal.auth.dto.OAuthAttributes;
import com.itmal.auth.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final UserMapper userMapper;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.of(provider, userNameAttributeName, oidcUser.getAttributes());
        log.info("[OAuth] provider={}, email={}, nickname={}", provider, attributes.getEmail(), attributes.getNickname());

        if (attributes.getEmail() == null || attributes.getEmail().isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_found"),
                    "이메일 정보를 가져올 수 없습니다."
            );
        }

        try {
            User user = getOrSaveUser(attributes);
            log.info("[OAuth] 로그인 성공 - userId={}, email={}", user.getUserId(), user.getEmail());
            return new CustomUserDetails(user, oidcUser.getAttributes(), oidcUser.getIdToken(), oidcUser.getUserInfo());
        } catch (Exception e) {
            log.error("[OAuth] 사용자 저장/조회 실패: {}", e.getMessage(), e);
            throw new OAuth2AuthenticationException(new OAuth2Error("user_save_failed"), e.getMessage(), e);
        }
    }

    private User getOrSaveUser(OAuthAttributes attributes) {
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
        for (int i = 0; i < 5; i++) {
            String candidate = base + "_" + UUID.randomUUID().toString().substring(0, 4);
            if (!userMapper.existsByNickname(candidate)) {
                return candidate;
            }
        }
        return base + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
