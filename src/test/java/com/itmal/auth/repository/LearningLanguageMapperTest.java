package com.itmal.auth.repository;

import com.itmal.auth.domain.Role;
import com.itmal.auth.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

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
    @DisplayName("존재하는 언어 이름으로 language_id 조회 성공")
    void findLanguageIdByName_whenExists_returnsId() {
        // Arrange & Act
        Long languageId = learningLanguageMapper.findLanguageIdByName("영어");

        // Assert
        assertThat(languageId).isNotNull();
        assertThat(languageId).isPositive();
    }

    @Test
    @DisplayName("존재하지 않는 언어 이름 조회 → null")
    void findLanguageIdByName_whenNotExists_returnsNull() {
        // Arrange & Act
        Long languageId = learningLanguageMapper.findLanguageIdByName("존재하지않는언어");

        // Assert
        assertThat(languageId).isNull();
    }

    @Test
    @DisplayName("user_learning_language insert 성공")
    void insertUserLearningLanguage_success() {
        // Arrange
        Long userId = insertTestUser("lang@itmal.com", "언어테스터");
        Long languageId = learningLanguageMapper.findLanguageIdByName("일본어");

        // Act & Assert
        assertThat(languageId).isNotNull();
        learningLanguageMapper.insertUserLearningLanguage(userId, languageId);
        // 예외 없이 실행되면 성공
    }

    @Test
    @DisplayName("여러 학습 언어 insert 성공")
    void insertUserLearningLanguage_multipleLanguages() {
        // Arrange
        Long userId = insertTestUser("multi@itmal.com", "멀티언어");
        Long englishId = learningLanguageMapper.findLanguageIdByName("영어");
        Long japaneseId = learningLanguageMapper.findLanguageIdByName("일본어");
        Long chineseId = learningLanguageMapper.findLanguageIdByName("중국어");

        // Act & Assert
        assertThat(englishId).isNotNull();
        assertThat(japaneseId).isNotNull();
        assertThat(chineseId).isNotNull();
        learningLanguageMapper.insertUserLearningLanguage(userId, englishId);
        learningLanguageMapper.insertUserLearningLanguage(userId, japaneseId);
        learningLanguageMapper.insertUserLearningLanguage(userId, chineseId);
    }
}
