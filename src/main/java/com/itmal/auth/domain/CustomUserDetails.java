package com.itmal.auth.domain;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CustomUserDetails implements UserDetails, OAuth2User, OidcUser {

    @Serial
    private static final long serialVersionUID = 1L;

    private final User user;
    private final Map<String, Object> oauthAttributes;
    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;

    public CustomUserDetails(User user) {
        this.user = user;
        this.oauthAttributes = Map.of();
        this.idToken = null;
        this.userInfo = null;
    }

    public CustomUserDetails(User user, Map<String, Object> oauthAttributes) {
        this.user = user;
        this.oauthAttributes = oauthAttributes;
        this.idToken = null;
        this.userInfo = null;
    }

    public CustomUserDetails(User user, Map<String, Object> oauthAttributes, OidcIdToken idToken, OidcUserInfo userInfo) {
        this.user = user;
        this.oauthAttributes = oauthAttributes;
        this.idToken = idToken;
        this.userInfo = userInfo;
    }

    public Long getUserId() {
        return user.getUserId();
    }

    public String getNickname() {
        return user.getNickname();
    }

    public String getNativeLanguage() {
        return user.getNativeLanguage();
    }

    public boolean isSocialUser() {
        return user.isSocialUser();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(user.getRole().name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !user.isDeleted();
    }

    // OAuth2User
    @Override
    public Map<String, Object> getAttributes() {
        return oauthAttributes;
    }

    @Override
    public String getName() {
        return String.valueOf(user.getUserId());
    }

    // OidcUser
    @Override
    public Map<String, Object> getClaims() {
        return idToken != null ? idToken.getClaims() : Map.of();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return userInfo;
    }

    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }
}
