package com.itmal.auth.service;

import com.itmal.auth.domain.Role;
import com.itmal.auth.domain.User;
import com.itmal.auth.dto.RegisterRequest;
import com.itmal.auth.exception.DuplicateEmailException;
import com.itmal.auth.exception.DuplicateNicknameException;
import com.itmal.auth.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(RegisterRequest request) {
        if (userMapper.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException();
        }

        if (userMapper.existsByNickname(request.getNickname())) {
            throw new DuplicateNicknameException();
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .nativeLanguage(request.getNativeLanguage())
                .role(Role.ROLE_USER)
                .emailVerified(false)
                .build();
        userMapper.insert(user); // useGeneratedKeys → user.getUserId() 자동 세팅
        userService.registerLearningLanguages(user.getUserId(), request.getLearningLanguages());
    }
}
