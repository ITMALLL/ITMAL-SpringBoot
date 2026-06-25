package com.itmal.auth.repository;

import com.itmal.auth.domain.Role;
import com.itmal.auth.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    private User buildTestUser(String email, String nickname) {
        return User.builder()
                .email(email)
                .password("encoded_password")
                .nickname(nickname)
                .nativeLanguage("ko")
                .role(Role.ROLE_USER)
                .emailVerified(false)
                .build();
    }

    @Test
    @DisplayName("유저 insert 후 이메일로 조회 성공")
    void insert_andFindByEmail() {
        // Arrange
        User user = buildTestUser("test@itmal.com", "테스터");

        // Act
        userMapper.insert(user);
        Optional<User> result = userMapper.findByEmail("test@itmal.com");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@itmal.com");
        assertThat(result.get().getNickname()).isEqualTo("테스터");
        assertThat(result.get().getUserId()).isNotNull();
    }

    @Test
    @DisplayName("존재하는 이메일 중복 체크 → true")
    void existsByEmail_whenExists_returnsTrue() {
        // Arrange
        userMapper.insert(buildTestUser("dup@itmal.com", "중복유저"));

        // Act
        boolean result = userMapper.existsByEmail("dup@itmal.com");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 이메일 중복 체크 → false")
    void existsByEmail_whenNotExists_returnsFalse() {
        // Arrange & Act
        boolean result = userMapper.existsByEmail("notexist@itmal.com");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("존재하는 닉네임 중복 체크 → true")
    void existsByNickname_whenExists_returnsTrue() {
        // Arrange
        userMapper.insert(buildTestUser("nick@itmal.com", "중복닉네임"));

        // Act
        boolean result = userMapper.existsByNickname("중복닉네임");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 닉네임 중복 체크 → false")
    void existsByNickname_whenNotExists_returnsFalse() {
        // Arrange & Act
        boolean result = userMapper.existsByNickname("없는닉네임");

        // Assert
        assertThat(result).isFalse();
    }
}
