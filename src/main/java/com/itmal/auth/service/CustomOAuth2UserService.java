package com.itmal.auth.service;

import com.itmal.auth.domain.CustomUserDetails;
import com.itmal.auth.domain.User;
import com.itmal.auth.dto.OAuthAttributes;
import com.itmal.auth.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserMapper userMapper;
    private final OAuthUserProcessor oAuthUserProcessor;
    private final RestClient restClient = RestClient.builder()
            .requestFactory(new SimpleClientHttpRequestFactory() {{
                setConnectTimeout(Duration.ofSeconds(5));
                setReadTimeout(Duration.ofSeconds(5));
            }})
            .build();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User;
        try {
            oAuth2User = super.loadUser(userRequest);
        } catch (Exception e) {
            log.error("[OAuth] 사용자 정보 조회 실패: {}", e.getMessage(), e);
            throw e;
        }

        String provider = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.of(provider, userNameAttributeName, oAuth2User.getAttributes());

        // GitHub 비공개 이메일 대응: /user/emails API 추가 호출
        if ("github".equals(provider) && (attributes.getEmail() == null || attributes.getEmail().isBlank())) {
            String fetchedEmail = fetchGitHubPrimaryEmail(userRequest.getAccessToken().getTokenValue());
            attributes = attributes.toBuilder().email(fetchedEmail).build();
        }

        log.info("[OAuth] provider={}, nickname={}", provider, attributes.getNickname());

        if (attributes.getEmail() == null || attributes.getEmail().isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_found"),
                    "이메일 정보를 가져올 수 없습니다. GitHub 이메일 설정을 확인해주세요."
            );
        }

        try {
            User user = oAuthUserProcessor.getOrSaveUser(attributes);
            log.info("[OAuth] 로그인 성공 - userId={}", user.getUserId());
            return new CustomUserDetails(user, attributes.getAttributes());
        } catch (Exception e) {
            log.error("[OAuth] 사용자 저장/조회 실패: {}", e.getMessage(), e);
            throw new OAuth2AuthenticationException(new OAuth2Error("user_save_failed"), e.getMessage(), e);
        }
    }

    private String fetchGitHubPrimaryEmail(String accessToken) {
        try {
            List<Map<String, Object>> emails = restClient.get()
                    .uri("https://api.github.com/user/emails")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (emails == null) return null;

            return emails.stream()
                    .filter(e -> Boolean.TRUE.equals(e.get("primary")) && Boolean.TRUE.equals(e.get("verified")))
                    .map(e -> (String) e.get("email"))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.warn("[OAuth] GitHub 이메일 API 조회 실패: {}", e.getMessage());
            return null;
        }
    }
}
