package com.itmal.answer.controller;

import com.itmal.answer.domain.Answer;
import com.itmal.answer.service.AnswerService;
import com.itmal.auth.repository.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AnswerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AnswerService answerService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AnswerController answerController;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(answerController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setValidator(validator)
                .build();

        var mockUser = User.withUsername("test@test.com").password("").roles("USER").build();
        var auth = new UsernamePasswordAuthenticationToken(mockUser, null, List.of());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("수정 페이지 조회 성공")
    void editForm_success() throws Exception {
        // given
        Answer answer = new Answer();
        answer.setAnswerId(1L);
        answer.setContent("기존 답변 내용");
        when(answerService.getAnswer(anyLong())).thenReturn(answer);

        // when & then
        mockMvc.perform(get("/answers/1/edit").param("questionId", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("answers/edit"))
                .andExpect(model().attributeExists("answer"))
                .andExpect(model().attributeExists("questionId"));
    }

    @Test
    @DisplayName("수정 실패 - 내용이 비어있으면 수정 페이지로 돌아온다")
    void editAnswer_emptyContent() throws Exception {
        // given
        Answer answer = new Answer();
        answer.setAnswerId(1L);
        when(answerService.getAnswer(anyLong())).thenReturn(answer);

        // when & then
        mockMvc.perform(post("/answers/1/edit")
                        .param("questionId", "5")
                        .param("content", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("answers/edit"));
    }

    @Test
    @DisplayName("수정 실패 - 2000자 초과 시 수정 페이지로 돌아온다")
    void editAnswer_contentTooLong() throws Exception {
        // given
        Answer answer = new Answer();
        answer.setAnswerId(1L);
        when(answerService.getAnswer(anyLong())).thenReturn(answer);
        String longContent = "a".repeat(2001);

        // when & then
        mockMvc.perform(post("/answers/1/edit")
                        .param("questionId", "5")
                        .param("content", longContent))
                .andExpect(status().isOk())
                .andExpect(view().name("answers/edit"));
    }

    @Test
    @DisplayName("수정 실패 시 원래 저장된 내용이 아니라 사용자가 입력한 내용을 그대로 보여준다")
    void editAnswer_validationFailure_keepsUserInput() throws Exception {
        // given
        Answer savedAnswer = new Answer();
        savedAnswer.setAnswerId(1L);
        savedAnswer.setContent("기존 답변 내용");
        when(answerService.getAnswer(anyLong())).thenReturn(savedAnswer);
        String invalidContent = "a".repeat(2001);

        // when & then
        mockMvc.perform(post("/answers/1/edit")
                        .param("questionId", "5")
                        .param("content", invalidContent))
                .andExpect(status().isOk())
                .andExpect(view().name("answers/edit"))
                .andExpect(model().attribute("answer",
                        org.hamcrest.Matchers.hasProperty("content", org.hamcrest.Matchers.is(invalidContent))))
                .andExpect(model().attributeHasFieldErrors("answerUpdateRequest", "content"));
    }
}
