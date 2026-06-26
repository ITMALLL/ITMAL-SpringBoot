package com.itmal.global.config;

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

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService, CustomOidcUserService customOidcUserService) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.customOidcUserService = customOidcUserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    request -> request.getServletPath().startsWith("/api/")
                )
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/register/social").permitAll()
                .requestMatchers("/questions", "/questions/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
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
                .defaultSuccessUrl("/", true)
                .failureHandler((request, response, exception) -> {
                    log.error("[OAuth] 로그인 실패: {}", exception.getMessage(), exception);
                    response.sendRedirect("/login?error");
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
