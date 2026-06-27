package com.itmal.auth.service;

import com.itmal.auth.domain.User;
import com.itmal.auth.dto.PasswordChangeRequest;
import com.itmal.auth.dto.ProfileUpdateRequest;
import com.itmal.auth.dto.SocialRegisterRequest;
import com.itmal.auth.exception.DuplicateNicknameException;
import com.itmal.auth.repository.LearningLanguageMapper;
import org.springframework.dao.DuplicateKeyException;
import com.itmal.auth.repository.UserMapper;
import com.itmal.question.dto.LanguageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final LearningLanguageMapper learningLanguageMapper;
    private final PasswordEncoder passwordEncoder;

    public boolean existsByEmail(String email) {
        return userMapper.existsByEmail(email);
    }

    public boolean existsByNickname(String nickname) {
        return userMapper.existsByNickname(nickname);
    }

    public User findById(Long userId) {
        return userMapper.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. userId=" + userId));
    }

    public List<String> getLearningLanguages(Long userId) {
        return learningLanguageMapper.findLanguageNamesByUserId(userId);
    }

    public List<LanguageDto> getAllLanguages() {
        return learningLanguageMapper.findAllLanguages();
    }

    @Transactional
    public void registerLearningLanguages(Long userId, List<String> names) {
        if (names != null && !names.isEmpty()) {
            learningLanguageMapper.insertUserLearningLanguagesByNames(userId, names);
        }
    }

    @Transactional
    public void updateProfile(Long userId, ProfileUpdateRequest request) {
        applyProfileUpdate(userId, request.getNickname(), request.getNativeLanguage(), request.getLearningLanguages());
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. userId=" + userId));

        if (user.isSocialUser()) {
            throw new IllegalStateException("소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        userMapper.updatePassword(userId, passwordEncoder.encode(request.getNewPassword()));
    }

    @Transactional
    public void completeSocialProfile(Long userId, SocialRegisterRequest request) {
        applyProfileUpdate(userId, request.getNickname(), request.getNativeLanguage(), request.getLearningLanguages());
    }

    private void applyProfileUpdate(Long userId, String nickname, String nativeLanguage, List<String> languageNames) {
        User current = userMapper.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. userId=" + userId));
        if (!current.getNickname().equals(nickname) && userMapper.existsByNickname(nickname)) {
            throw new DuplicateNicknameException();
        }
        try {
            userMapper.updateProfile(userId, nickname, nativeLanguage);
        } catch (DuplicateKeyException e) {
            throw new DuplicateNicknameException();
        }
        saveLanguages(userId, languageNames);
    }

    @Transactional
    public void deleteAccount(Long userId) {
        userMapper.softDelete(userId);
    }

    private void saveLanguages(Long userId, List<String> languageNames) {
        learningLanguageMapper.deleteByUserId(userId);
        if (languageNames != null && !languageNames.isEmpty()) {
            learningLanguageMapper.insertUserLearningLanguagesByNames(userId, languageNames);
        }
    }
}
