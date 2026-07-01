package com.itmal.auth.service;

import com.itmal.auth.domain.Role;
import com.itmal.auth.domain.User;
import com.itmal.auth.dto.PasswordChangeRequest;
import com.itmal.auth.dto.ProfileUpdateRequest;
import com.itmal.auth.dto.SocialRegisterRequest;
import com.itmal.auth.exception.DuplicateNicknameException;
import com.itmal.auth.repository.UserMapper;
import com.itmal.global.exception.ApiException;
import com.itmal.question.dto.LanguageDto;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String RAW_PASSWORD = "password123!";

    private User insertNormalUser(String email, String nickname) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(RAW_PASSWORD))
                .nickname(nickname)
                .nativeLanguage("ko")
                .role(Role.ROLE_USER)
                .emailVerified(true)
                .build();
        userMapper.insert(user);
        return userMapper.findByEmail(email).orElseThrow();
    }

    private User insertSocialUser(String email, String nickname) {
        User user = User.builder()
                .email(email)
                .nickname(nickname)
                .provider("github")
                .providerId("12345")
                .role(Role.ROLE_USER)
                .emailVerified(true)
                .build();
        userMapper.insert(user);
        return userMapper.findByEmail(email).orElseThrow();
    }

    // ===== 프로필 수정 =====

    @Test
    @DisplayName("닉네임/모국어/학습언어 변경 성공")
    void updateProfile_success() {
        // Arrange
        User user = insertNormalUser("profile@itmal.com", "기존닉네임");
        ProfileUpdateRequest request = new ProfileUpdateRequest("새닉네임", "en", List.of("영어", "일본어"));

        // Act
        userService.updateProfile(user.getUserId(), request);

        // Assert
        User updated = userMapper.findByEmail("profile@itmal.com").orElseThrow();
        assertThat(updated.getNickname()).isEqualTo("새닉네임");
        assertThat(updated.getNativeLanguage()).isEqualTo("en");

        List<LanguageDto> langs = userService.getLearningLanguages(user.getUserId());
        assertThat(langs).extracting(LanguageDto::getLanguageName).containsExactlyInAnyOrder("영어", "일본어");
    }

    @Test
    @DisplayName("닉네임 중복 시 예외 발생")
    void updateProfile_duplicateNickname_throwsException() {
        // Arrange
        insertNormalUser("user1@itmal.com", "중복닉네임");
        User user2 = insertNormalUser("user2@itmal.com", "다른닉네임");
        ProfileUpdateRequest request = new ProfileUpdateRequest("중복닉네임", "ko", List.of());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateProfile(user2.getUserId(), request))
                .isInstanceOf(DuplicateNicknameException.class);
    }

    // ===== 비밀번호 변경 =====

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changePassword_success() {
        // Arrange
        User user = insertNormalUser("pw@itmal.com", "유저");
        PasswordChangeRequest request = new PasswordChangeRequest("password123!", "newPassword123!", "newPassword123!");

        // Act & Assert (예외 없이 통과)
        userService.changePassword(user.getUserId(), request);
    }

    @Test
    @DisplayName("현재 비밀번호 불일치 시 예외 발생")
    void changePassword_wrongCurrentPassword_throwsException() {
        // Arrange
        User user = insertNormalUser("pw2@itmal.com", "유저2");
        PasswordChangeRequest request = new PasswordChangeRequest("wrongPassword!", "newPassword123!", "newPassword123!");

        // Act & Assert
        assertThatThrownBy(() -> userService.changePassword(user.getUserId(), request))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("소셜 유저 비밀번호 변경 시 예외 발생")
    void changePassword_socialUser_throwsException() {
        // Arrange
        User user = insertSocialUser("social@itmal.com", "소셜유저");
        PasswordChangeRequest request = new PasswordChangeRequest("", "newPassword123!", "newPassword123!");

        // Act & Assert
        assertThatThrownBy(() -> userService.changePassword(user.getUserId(), request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.");
    }

    // ===== 소셜 초기설정 =====

    @Test
    @DisplayName("소셜 초기설정 성공")
    void completeSocialProfile_success() {
        // Arrange
        User user = insertSocialUser("social-setup@itmal.com", "초기닉네임");
        SocialRegisterRequest request = new SocialRegisterRequest();
        request.setNickname("설정닉네임");
        request.setNativeLanguage("ko");
        request.setLearningLanguages(List.of("영어", "일본어"));

        // Act
        userService.completeSocialProfile(user.getUserId(), request);

        // Assert
        User updated = userMapper.findByEmail("social-setup@itmal.com").orElseThrow();
        assertThat(updated.getNickname()).isEqualTo("설정닉네임");
        assertThat(updated.getNativeLanguage()).isEqualTo("ko");

        List<LanguageDto> langs = userService.getLearningLanguages(user.getUserId());
        assertThat(langs).extracting(LanguageDto::getLanguageName).containsExactlyInAnyOrder("영어", "일본어");
    }

    @Test
    @DisplayName("소셜 초기설정 닉네임 중복 시 예외 발생")
    void completeSocialProfile_duplicateNickname_throwsException() {
        // Arrange
        insertNormalUser("existing@itmal.com", "기존닉네임");
        User socialUser = insertSocialUser("social2@itmal.com", "소셜유저");
        SocialRegisterRequest request = new SocialRegisterRequest();
        request.setNickname("기존닉네임");
        request.setNativeLanguage("ko");
        request.setLearningLanguages(List.of("영어"));

        // Act & Assert
        assertThatThrownBy(() -> userService.completeSocialProfile(socialUser.getUserId(), request))
                .isInstanceOf(DuplicateNicknameException.class);
    }

    // ===== 회원탈퇴 =====

    @Test
    @DisplayName("회원탈퇴 성공 - deleted_at 설정됨")
    void deleteAccount_success() {
        // Arrange
        User user = insertNormalUser("delete@itmal.com", "탈퇴유저");

        // Act
        userService.deleteAccount(user.getUserId());

        // Assert
        User deleted = userMapper.findById(user.getUserId()).orElseThrow();
        assertThat(deleted.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("소셜 유저 회원탈퇴 성공 - deleted_at 설정됨")
    void deleteAccount_socialUser_success() {
        // Arrange
        User user = insertSocialUser("social-delete@itmal.com", "소셜탈퇴");

        // Act
        userService.deleteAccount(user.getUserId());

        // Assert
        User deleted = userMapper.findById(user.getUserId()).orElseThrow();
        assertThat(deleted.isDeleted()).isTrue();
    }
}
