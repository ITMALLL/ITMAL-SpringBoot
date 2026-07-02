package com.itmal.tutorapplication.service;

import com.itmal.auth.domain.Role;
import com.itmal.auth.domain.User;
import com.itmal.auth.repository.UserMapper;
import com.itmal.global.exception.BusinessException;
import com.itmal.global.exception.ErrorCode;
import com.itmal.tutorapplication.domain.ApplicationStatus;
import com.itmal.tutorapplication.domain.TutorApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class TutorApplicationServiceTest {

    @Autowired
    private TutorApplicationService tutorApplicationService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User insertUser(String email, String nickname, Role role) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode("password123!"))
                .nickname(nickname)
                .nativeLanguage("ko")
                .role(role)
                .emailVerified(true)
                .build();
        userMapper.insert(user);
        return userMapper.findByEmail(email).orElseThrow();
    }

    // ===== 튜터 신청 =====

    @Test
    @DisplayName("튜터 신청 성공")
    void apply_success() {
        // Arrange
        User user = insertUser("apply@itmal.com", "신청유저", Role.ROLE_USER);

        // Act
        tutorApplicationService.apply(user.getUserId());

        // Assert
        List<TutorApplication> list = tutorApplicationService.getPendingApplications();
        assertThat(list).anyMatch(a -> a.getUserId().equals(user.getUserId()));
    }

    @Test
    @DisplayName("이미 신청 중인 경우 예외 발생")
    void apply_alreadyApplied_throwsException() {
        // Arrange
        User user = insertUser("dup@itmal.com", "중복신청유저", Role.ROLE_USER);
        tutorApplicationService.apply(user.getUserId());

        // Act & Assert
        assertThatThrownBy(() -> tutorApplicationService.apply(user.getUserId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_APPLIED);
    }

    @Test
    @DisplayName("이미 튜터인 경우 예외 발생")
    void apply_alreadyTutor_throwsException() {
        // Arrange
        User user = insertUser("tutor@itmal.com", "기존튜터", Role.ROLE_TUTOR);

        // Act & Assert
        assertThatThrownBy(() -> tutorApplicationService.apply(user.getUserId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_TUTOR);
    }

    // ===== 승인 =====

    @Test
    @DisplayName("승인 성공 - role이 ROLE_TUTOR로 변경됨")
    void approve_success() {
        // Arrange
        User user = insertUser("approve@itmal.com", "승인유저", Role.ROLE_USER);
        tutorApplicationService.apply(user.getUserId());
        Long applicationId = tutorApplicationService.getPendingApplications().stream()
                .filter(a -> a.getUserId().equals(user.getUserId()))
                .findFirst().orElseThrow().getTutorApplicationId();

        // Act
        tutorApplicationService.approve(applicationId);

        // Assert
        User updated = userMapper.findById(user.getUserId()).orElseThrow();
        assertThat(updated.getRole()).isEqualTo(Role.ROLE_TUTOR);
    }

    @Test
    @DisplayName("존재하지 않는 신청 승인 시 예외 발생")
    void approve_notFound_throwsException() {
        // Act & Assert
        assertThatThrownBy(() -> tutorApplicationService.approve(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TUTOR_APPLICATION_NOT_FOUND);
    }

    // ===== 거절 =====

    @Test
    @DisplayName("거절 성공 - status가 REJECTED로 변경됨")
    void reject_success() {
        // Arrange
        User user = insertUser("reject@itmal.com", "거절유저", Role.ROLE_USER);
        tutorApplicationService.apply(user.getUserId());
        Long applicationId = tutorApplicationService.getPendingApplications().stream()
                .filter(a -> a.getUserId().equals(user.getUserId()))
                .findFirst().orElseThrow().getTutorApplicationId();

        // Act
        tutorApplicationService.reject(applicationId);

        // Assert
        List<TutorApplication> pending = tutorApplicationService.getPendingApplications();
        assertThat(pending).noneMatch(a -> a.getUserId().equals(user.getUserId()));
    }

    @Test
    @DisplayName("존재하지 않는 신청 거절 시 예외 발생")
    void reject_notFound_throwsException() {
        // Act & Assert
        assertThatThrownBy(() -> tutorApplicationService.reject(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TUTOR_APPLICATION_NOT_FOUND);
    }

    // ===== 목록 조회 =====

    @Test
    @DisplayName("PENDING 목록 조회 - 승인/거절된 건 제외")
    void getPendingApplications_excludesProcessed() {
        // Arrange
        User user1 = insertUser("list1@itmal.com", "목록유저1", Role.ROLE_USER);
        User user2 = insertUser("list2@itmal.com", "목록유저2", Role.ROLE_USER);
        tutorApplicationService.apply(user1.getUserId());
        tutorApplicationService.apply(user2.getUserId());

        Long appId = tutorApplicationService.getPendingApplications().stream()
                .filter(a -> a.getUserId().equals(user1.getUserId()))
                .findFirst().orElseThrow().getTutorApplicationId();
        tutorApplicationService.approve(appId);

        // Act
        List<TutorApplication> pending = tutorApplicationService.getPendingApplications();

        // Assert
        assertThat(pending).noneMatch(a -> a.getUserId().equals(user1.getUserId()));
        assertThat(pending).anyMatch(a -> a.getUserId().equals(user2.getUserId()));
    }
}
