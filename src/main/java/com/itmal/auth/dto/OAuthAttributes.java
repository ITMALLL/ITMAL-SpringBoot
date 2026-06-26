package com.itmal.auth.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder(toBuilder = true)
public class OAuthAttributes {

    private Map<String, Object> attributes;
    private String email;
    private String nickname;
    private String provider;
    private String providerId;

    public static OAuthAttributes of(String provider, String userNameAttributeName, Map<String, Object> attributes) {
        return switch (provider) {
            case "github" -> ofGitHub(attributes);
            case "google" -> ofGoogle(attributes);
            default -> throw new IllegalArgumentException("지원하지 않는 OAuth2 제공자: " + provider);
        };
    }

    private static OAuthAttributes ofGitHub(Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .email((String) attributes.get("email"))
                .nickname((String) attributes.get("login"))
                .provider("github")
                .providerId(String.valueOf(attributes.get("id")))
                .attributes(attributes)
                .build();
    }

    private static OAuthAttributes ofGoogle(Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .email((String) attributes.get("email"))
                .nickname((String) attributes.get("name"))
                .provider("google")
                .providerId((String) attributes.get("sub"))
                .attributes(attributes)
                .build();
    }
}
