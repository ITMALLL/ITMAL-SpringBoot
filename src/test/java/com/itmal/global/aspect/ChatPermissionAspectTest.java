package com.itmal.global.aspect;

import com.itmal.auth.domain.Role;
import com.itmal.auth.domain.User;
import com.itmal.auth.repository.UserMapper;
import com.itmal.chat.dto.ChatRequestDto;
import com.itmal.chat.service.ChatRequestService;
import com.itmal.global.exception.ApiException;
import com.itmal.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

@SpringBootTest
@Transactional
class ChatPermissionAspectTest {

    @Autowired
    private ChatRequestService chatRequestService;

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

    private ChatRequestDto buildRequest(Long requesterId, Long responderId) {
        ChatRequestDto dto = new ChatRequestDto();
        dto.setRequesterId(requesterId);
        dto.setResponderId(responderId);
        dto.setIntroMessage("안녕하세요, 튜터링 요청드립니다.");
        return dto;
    }

    @Test
    @DisplayName("USER에게 채팅 요청 시 CHAT_INVALID_TARGET 예외 발생")
    void createChatRequest_responderIsUser_throwsException() {
        // Arrange
        User requester = insertUser("requester@itmal.com", "요청자", Role.ROLE_USER);
        User responder = insertUser("responder@itmal.com", "일반유저", Role.ROLE_USER);

        // Act & Assert
        assertThatThrownBy(() -> chatRequestService.createChatRequest(buildRequest(requester.getUserId(), responder.getUserId())))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_INVALID_TARGET);
    }

    @Test
    @DisplayName("TUTOR에게 채팅 요청 시 권한 검증 통과")
    void createChatRequest_responderIsTutor_passes() {
        // Arrange
        User requester = insertUser("requester2@itmal.com", "요청자2", Role.ROLE_USER);
        User tutor = insertUser("tutor@itmal.com", "튜터", Role.ROLE_TUTOR);

        // Act & Assert
        assertThatNoException().isThrownBy(
                () -> chatRequestService.createChatRequest(buildRequest(requester.getUserId(), tutor.getUserId()))
        );
    }
}
