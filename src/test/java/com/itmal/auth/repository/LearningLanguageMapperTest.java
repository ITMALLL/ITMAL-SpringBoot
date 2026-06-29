package com.itmal.auth.repository;

import com.itmal.auth.domain.Role;
import com.itmal.auth.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class LearningLanguageMapperTest {

    @Autowired
    private LearningLanguageMapper learningLanguageMapper;

    @Autowired
    private UserMapper userMapper;

    private Long insertTestUser(String email, String nickname) {
        User user = User.builder()
                .email(email)
                .password("encoded_password")
                .nickname(nickname)
                .nativeLanguage("ko")
                .role(Role.ROLE_USER)
                .emailVerified(false)
                .build();
        userMapper.insert(user);
        return userMapper.findByEmail(email)
                .map(User::getUserId)
                .orElseThrow();
    }

    @Test
    @DisplayName("배치 insert 후 학습 언어 조회 성공")
    void insertUserLearningLanguagesByNames_success() {
        // Arrange
        Long userId = insertTestUser("lang@itmal.com", "언어테스터");

        // Act
        learningLanguageMapper.insertUserLearningLanguagesByNames(userId, List.of("영어", "일본어"));

        // Assert
        List<String> result = learningLanguageMapper.findLanguageNamesByUserId(userId);
        assertThat(result).containsExactlyInAnyOrder("영어", "일본어");
    }

    @Test
    @DisplayName("학습 언어 삭제 후 재등록 성공")
    void deleteByUserId_thenReinsert_success() {
        // Arrange
        Long userId = insertTestUser("reset@itmal.com", "리셋유저");
        learningLanguageMapper.insertUserLearningLanguagesByNames(userId, List.of("영어", "중국어"));

        // Act
        learningLanguageMapper.deleteByUserId(userId);
        learningLanguageMapper.insertUserLearningLanguagesByNames(userId, List.of("일본어"));

        // Assert
        List<String> result = learningLanguageMapper.findLanguageNamesByUserId(userId);
        assertThat(result).containsExactly("일본어");
    }

    @Test
    @DisplayName("전체 언어 목록 조회 성공")
    void findAllLanguages_returnsNonEmptyList() {
        // Act & Assert
        assertThat(learningLanguageMapper.findAllLanguages()).isNotEmpty();
    }
}
