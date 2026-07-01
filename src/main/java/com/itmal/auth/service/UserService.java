package com.itmal.auth.service;

import com.itmal.auth.domain.User;
import com.itmal.auth.dto.PasswordChangeRequest;
import com.itmal.auth.dto.ProfileUpdateRequest;
import com.itmal.auth.dto.SocialRegisterRequest;
import com.itmal.auth.exception.DuplicateNicknameException;
import com.itmal.auth.repository.LearningLanguageMapper;
import com.itmal.auth.repository.UserMapper;
import com.itmal.global.exception.ApiException;
import com.itmal.global.exception.ErrorCode;
import com.itmal.question.dto.LanguageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final LearningLanguageMapper learningLanguageMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetTokenStore passwordResetTokenStore;

    public boolean existsByEmail(String email) {
        return userMapper.existsByEmail(email);
    }

    public boolean existsByNickname(String nickname) {
        return userMapper.existsByNickname(nickname);
    }

    public User findById(Long userId) {
        return userMapper.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    public List<LanguageDto> getLearningLanguages(Long userId) {
        return learningLanguageMapper.findLanguageCodesByUserId(userId);
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
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (user.isSocialUser()) {
            throw new IllegalStateException("소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ApiException(ErrorCode.PASSWORD_MISMATCH);
        }

        userMapper.updatePassword(userId, passwordEncoder.encode(request.getNewPassword()));
    }

    @Transactional
    public void completeSocialProfile(Long userId, SocialRegisterRequest request) {
        applyProfileUpdate(userId, request.getNickname(), request.getNativeLanguage(), request.getLearningLanguages());
    }

    private void applyProfileUpdate(Long userId, String nickname, String nativeLanguage, List<String> languageNames) {
        User current = userMapper.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
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

    public void initiatePasswordReset(String email, String resetBaseUrl) {
        userMapper.findByEmail(email)
                .filter(u -> !u.isDeleted())
                .filter(u -> !u.isSocialUser())
                .ifPresent(user -> {
                    String token = passwordResetTokenStore.save(email);
                    String resetUrl = resetBaseUrl + "?token=" + token;
                    emailVerificationService.sendPasswordResetEmail(email, resetUrl);
                    log.info("[비밀번호 찾기] 재설정 링크 발송 - userId={}", user.getUserId());
                });
    }

    @Transactional
    public void confirmPasswordReset(String token, String newPassword) {
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 8) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        String email = passwordResetTokenStore.consumeEmail(token)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST));

        User user = userMapper.findByEmail(email)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        userMapper.updatePassword(user.getUserId(), passwordEncoder.encode(newPassword));
        log.info("[비밀번호 찾기] 비밀번호 변경 완료 - userId={}", user.getUserId());
    }

    public boolean isValidResetToken(String token) {
        return passwordResetTokenStore.findEmail(token).isPresent();
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
