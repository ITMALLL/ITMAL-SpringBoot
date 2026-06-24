package com.itmal.auth.service;

import com.itmal.auth.domain.Role;
import com.itmal.auth.domain.User;
import com.itmal.auth.dto.RegisterRequest;
import com.itmal.auth.exception.DuplicateEmailException;
import com.itmal.auth.repository.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입 성공 - DB에 유저 저장 확인")
    void register_success() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("register-test@test.com");
        request.setPassword("Test1234!");
        request.setPasswordConfirm("Test1234!");
        request.setNickname("테스트유저");
        request.setNativeLanguage("ko");

        // Act
        authService.register(request);

        // Assert
        Optional<User> saved = userMapper.findByEmail("register-test@test.com");
        assertThat(saved).isPresent();
        assertThat(saved.get().getEmail()).isEqualTo("register-test@test.com");
        assertThat(saved.get().getNickname()).isEqualTo("테스트유저");
        assertThat(saved.get().getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(saved.get().isEmailVerified()).isFalse();
        assertThat(passwordEncoder.matches("Test1234!", saved.get().getPassword())).isTrue();
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 이메일")
    void register_duplicateEmail() {
        // Arrange
        RegisterRequest first = new RegisterRequest();
        first.setEmail("duplicate@test.com");
        first.setPassword("Test1234!");
        first.setPasswordConfirm("Test1234!");
        first.setNickname("첫번째유저");
        first.setNativeLanguage("ko");
        authService.register(first);

        RegisterRequest duplicate = new RegisterRequest();
        duplicate.setEmail("duplicate@test.com");
        duplicate.setPassword("Test1234!");
        duplicate.setPasswordConfirm("Test1234!");
        duplicate.setNickname("두번째유저");
        duplicate.setNativeLanguage("ko");

        // Act & Assert
        assertThatThrownBy(() -> authService.register(duplicate))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    @DisplayName("회원가입 실패 - 비밀번호 불일치")
    void register_passwordMismatch() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("mismatch@test.com");
        request.setPassword("Test1234!");
        request.setPasswordConfirm("Wrong1234!");
        request.setNickname("테스트유저");
        request.setNativeLanguage("ko");

        // Act & Assert
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
    }
}
