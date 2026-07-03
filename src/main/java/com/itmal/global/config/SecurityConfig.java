package com.itmal.global.config;

import com.itmal.auth.handler.OAuth2LoginSuccessHandler;
import com.itmal.auth.service.CustomOAuth2UserService;
import com.itmal.auth.service.CustomOidcUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.remember-me.key}")
    private String rememberMeKey;

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService,
                          CustomOidcUserService customOidcUserService,
                          OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.customOidcUserService = customOidcUserService;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    request -> request.getServletPath().startsWith("/api/")
                )
                // 에디터 이미지 업로드: 미인증 시 로그인 HTML 리다이렉트 대신 JSON 401 반환
                // (프론트의 xhr responseType='json' 및 res.error.message 계약과 일치)
                .defaultAuthenticationEntryPointFor(
                    (request, response, authEx) -> {
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"error\":{\"message\":\"로그인이 필요합니다.\"}}");
                    },
                    request -> request.getServletPath().equals("/questions/upload-image")
                )
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    request.setAttribute("status", 403);
                    request.setAttribute("message", "접근 권한이 없습니다.");
                    request.getRequestDispatcher("/error/403").forward(request, response);
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/forgot-password", "/forgot-password/reset").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/tutor-applications").authenticated()
                .requestMatchers("/mypage/**", "/register/social").authenticated()
                .requestMatchers(HttpMethod.GET,  "/questions/write").authenticated()
                .requestMatchers(HttpMethod.POST, "/questions/write").authenticated()
                .requestMatchers(HttpMethod.POST, "/questions/upload-image").authenticated()
                .requestMatchers(HttpMethod.GET,  "/questions/editor-images/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/questions/*/delete").authenticated()
                .requestMatchers(HttpMethod.GET,  "/questions/*/edit").authenticated()
                .requestMatchers(HttpMethod.POST, "/questions/*/edit").authenticated()
                .requestMatchers(HttpMethod.GET,  "/answers/write").authenticated()
                .requestMatchers(HttpMethod.POST, "/answers").authenticated()
                .requestMatchers(HttpMethod.GET,  "/answers/*/edit").authenticated()
                .requestMatchers(HttpMethod.POST, "/answers/*/edit").authenticated()
                .requestMatchers(HttpMethod.POST, "/answers/*/delete").authenticated()
                .requestMatchers(HttpMethod.POST, "/answers/*/like").authenticated()
                .requestMatchers(HttpMethod.POST, "/answers/*/adopt").authenticated()
                .requestMatchers(HttpMethod.POST, "/answers/*/report").authenticated()
                .requestMatchers("/questions", "/questions/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/error/**").permitAll()
                .requestMatchers(HttpMethod.PUT, "/api/chat-request/*/accept", "/api/chat-request/*/reject").hasRole("TUTOR")
                .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/email/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/papago/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/**").authenticated()
                .anyRequest().permitAll() // TODO: 개발 완료 후 authenticated()로 복구
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .key(rememberMeKey)
                .tokenValiditySeconds(60 * 60 * 24 * 14) // 14일
            )
            .oauth2Login(oauth -> oauth
                .loginPage("/login")
                .successHandler(oAuth2LoginSuccessHandler)
                .failureHandler((request, response, exception) -> {
                    log.error("[OAuth] 로그인 실패: {}", exception.getMessage(), exception);
                    if (exception instanceof org.springframework.security.oauth2.core.OAuth2AuthenticationException oauthEx
                            && "reregistration_blocked".equals(oauthEx.getError().getErrorCode())) {
                        response.sendRedirect("/login?reregistration_blocked");
                    } else {
                        response.sendRedirect("/login?error");
                    }
                })
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)       // GitHub 등 non-OIDC
                    .oidcUserService(customOidcUserService)     // Google 등 OIDC
                )
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
