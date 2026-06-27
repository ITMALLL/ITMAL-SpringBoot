package com.itmal.auth.service;

import com.itmal.auth.dto.RegisterRequest;
import com.itmal.auth.exception.DuplicateEmailException;
import com.itmal.auth.exception.DuplicateNicknameException;
import com.itmal.auth.repository.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserMapper userMapper;
    private UserService userService;
    private PasswordEncoder passwordEncoder;
    private AuthService service;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        userService = mock(UserService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        service = new AuthService(userMapper, userService, passwordEncoder);
    }

    private RegisterRequest createRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("user@test.com");
        req.setPassword("Password1!");
        req.setPasswordConfirm("Password1!");
        req.setNickname("testnick");
        req.setNativeLanguage("한국어");
        req.setLearningLanguages(List.of("영어"));
        return req;
    }

    @Test
    void register_정상가입() {
        // Arrange
        RegisterRequest request = createRequest();
        when(userMapper.existsByEmail(request.getEmail())).thenReturn(false);
        when(userMapper.existsByNickname(request.getNickname())).thenReturn(false);

        // Act
        service.register(request);

        // Assert
        verify(userMapper).insert(any());
        verify(userService).registerLearningLanguages(any(), eq(request.getLearningLanguages()));
    }

    @Test
    void register_이메일중복_예외발생() {
        // Arrange
        RegisterRequest request = createRequest();
        when(userMapper.existsByEmail(request.getEmail())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateEmailException.class, () -> service.register(request));
        verify(userMapper, never()).insert(any());
    }

    @Test
    void register_닉네임중복_예외발생() {
        // Arrange
        RegisterRequest request = createRequest();
        when(userMapper.existsByEmail(request.getEmail())).thenReturn(false);
        when(userMapper.existsByNickname(request.getNickname())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateNicknameException.class, () -> service.register(request));
        verify(userMapper, never()).insert(any());
    }

    @Test
    void register_DB이메일중복키_예외변환() {
        // Arrange
        RegisterRequest request = createRequest();
        when(userMapper.existsByEmail(request.getEmail())).thenReturn(false);
        when(userMapper.existsByNickname(request.getNickname())).thenReturn(false);
        doThrow(new DuplicateKeyException("Duplicate entry for email"))
                .when(userMapper).insert(any());

        // Act & Assert
        assertThrows(DuplicateEmailException.class, () -> service.register(request));
    }

    @Test
    void register_DB닉네임중복키_예외변환() {
        // Arrange
        RegisterRequest request = createRequest();
        when(userMapper.existsByEmail(request.getEmail())).thenReturn(false);
        when(userMapper.existsByNickname(request.getNickname())).thenReturn(false);
        doThrow(new DuplicateKeyException("Duplicate entry for nickname"))
                .when(userMapper).insert(any());

        // Act & Assert
        assertThrows(DuplicateNicknameException.class, () -> service.register(request));
    }
}
