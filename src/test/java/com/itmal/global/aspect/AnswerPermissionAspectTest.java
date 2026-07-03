package com.itmal.global.aspect;

import com.itmal.answer.dto.AnswerCreateRequest;
import com.itmal.answer.service.AnswerService;
import com.itmal.auth.domain.Role;
import com.itmal.auth.domain.User;
import com.itmal.auth.repository.UserMapper;
import com.itmal.global.exception.ErrorCode;
import com.itmal.global.exception.ViewException;
import com.itmal.question.dto.QuestionDto;
import com.itmal.question.mapper.QuestionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AnswerPermissionAspectTest {

    @Autowired
    private AnswerService answerService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private QuestionMapper questionMapper;

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

    private Long insertQuestion(Long userId, String target) {
        QuestionDto dto = new QuestionDto();
        dto.setTitle("테스트 질문");
        dto.setContent("테스트 내용입니다.");
        dto.setUserId(userId);
        dto.setLanguageId(1L);
        dto.setTarget(target);
        questionMapper.insertQuestion(dto);
        return dto.getQuestionId();
    }

    private AnswerCreateRequest buildRequest(Long questionId) {
        AnswerCreateRequest request = new AnswerCreateRequest();
        request.setQuestionId(questionId);
        request.setContent("테스트 답변입니다.");
        return request;
    }

    @Test
    @DisplayName("튜터 전용 질문에 USER가 답변 시 FORBIDDEN 예외 발생")
    void createAnswer_tutorOnlyQuestion_userForbidden() {
        // Arrange
        User author = insertUser("author@itmal.com", "질문작성자", Role.ROLE_TUTOR);
        User user = insertUser("user@itmal.com", "일반유저", Role.ROLE_USER);
        Long questionId = insertQuestion(author.getUserId(), "TUTOR");

        // Act & Assert
        assertThatThrownBy(() -> answerService.createAnswer(buildRequest(questionId), user.getUserId()))
                .isInstanceOf(ViewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("튜터 전용 질문에 TUTOR가 답변 시 성공")
    void createAnswer_tutorOnlyQuestion_tutorAllowed() {
        // Arrange
        User tutorAuthor = insertUser("tutor-author@itmal.com", "튜터작성자", Role.ROLE_TUTOR);
        User tutorAnswerer = insertUser("tutor-answerer@itmal.com", "튜터답변자", Role.ROLE_TUTOR);
        Long questionId = insertQuestion(tutorAuthor.getUserId(), "TUTOR");

        // Act & Assert
        assertThatNoException().isThrownBy(
                () -> answerService.createAnswer(buildRequest(questionId), tutorAnswerer.getUserId())
        );
    }

    @Test
    @DisplayName("일반 질문에 USER가 답변 시 성공")
    void createAnswer_normalQuestion_userAllowed() {
        // Arrange
        User author = insertUser("author2@itmal.com", "질문작성자2", Role.ROLE_USER);
        User user = insertUser("user2@itmal.com", "일반유저2", Role.ROLE_USER);
        Long questionId = insertQuestion(author.getUserId(), null);

        // Act & Assert
        assertThatNoException().isThrownBy(
                () -> answerService.createAnswer(buildRequest(questionId), user.getUserId())
        );
    }
}
