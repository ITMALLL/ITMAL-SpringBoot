package com.itmal.auth.service;

import com.itmal.auth.domain.Role;
import com.itmal.auth.domain.User;
import com.itmal.auth.dto.RegisterRequest;
import com.itmal.auth.exception.DuplicateEmailException;
import com.itmal.auth.exception.DuplicateNicknameException;
import com.itmal.auth.repository.LearningLanguageMapper;
import com.itmal.auth.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final LearningLanguageMapper learningLanguageMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

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
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            String message = e.getMessage();
            if (message != null && message.contains("email")) {
                throw new DuplicateEmailException();
            }
            throw new DuplicateNicknameException();
        }

        Long userId = userMapper.findByEmail(request.getEmail())
                .map(User::getUserId)
                .orElseThrow(() -> new IllegalStateException("회원가입 후 사용자를 찾을 수 없습니다."));

        if (!request.getLearningLanguages().isEmpty()) {
            learningLanguageMapper.insertUserLearningLanguagesByNames(userId, request.getLearningLanguages());
        }
    }
}
