package com.itmal.auth.service;

import com.itmal.auth.domain.User;
import com.itmal.auth.dto.PasswordChangeRequest;
import com.itmal.auth.dto.ProfileUpdateRequest;
import com.itmal.auth.exception.DuplicateNicknameException;
import com.itmal.auth.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void updateProfile(Long userId, ProfileUpdateRequest request) {
        User current = userMapper.findById(userId).orElseThrow();
        if (!current.getNickname().equals(request.getNickname()) &&
                userMapper.existsByNickname(request.getNickname())) {
            throw new DuplicateNicknameException();
        }
        userMapper.updateProfile(userId, request.getNickname(), request.getNativeLanguage());
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = userMapper.findById(userId).orElseThrow();

        if (user.isSocialUser()) {
            throw new IllegalStateException("소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        }

        userMapper.updatePassword(userId, passwordEncoder.encode(request.getNewPassword()));
    }

    @Transactional
    public void deleteAccount(Long userId) {
        userMapper.softDelete(userId);
    }
}
