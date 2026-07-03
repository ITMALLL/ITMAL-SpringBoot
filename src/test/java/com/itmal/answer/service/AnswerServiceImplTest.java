package com.itmal.answer.service;

import com.itmal.answer.domain.Answer;
import com.itmal.answer.mapper.AnswerMapper;
import com.itmal.global.exception.ViewException;
import com.itmal.notification.service.NotificationService;
import com.itmal.question.dto.QuestionDto;
import com.itmal.question.mapper.QuestionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnswerServiceImplTest {

    @Mock
    private AnswerMapper answerMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private QuestionMapper questionMapper;

    @InjectMocks
    private AnswerServiceImpl answerService;

    @Test
    @DisplayName("userId로 내 답변 목록을 조회한다")
    void getAnswerByUserId_success() {
        // given
        Long userId = 1L;
        Answer answer = new Answer();
        answer.setUserId(userId);
        answer.setContent("테스트 답변");
        when(answerMapper.findByUserId(userId)).thenReturn(List.of(answer));

        // when
        List<Answer> result = answerService.getAnswerByUserId(userId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(userId);
        verify(answerMapper, times(1)).findByUserId(userId);
    }

    @Test
    @DisplayName("답변이 없으면 빈 리스트를 반환한다")
    void getAnswerByUserId_empty() {
        // given
        Long userId = 1L;
        when(answerMapper.findByUserId(userId)).thenReturn(List.of());

        // when
        List<Answer> result = answerService.getAnswerByUserId(userId);

        // then
        assertThat(result).isEmpty();
        verify(answerMapper, times(1)).findByUserId(userId);
    }

    @Test
    @DisplayName("채택된 답변이 없으면 정상적으로 채택된다")
    void adoptAnswer_success_whenNoAcceptedAnswerExists() {
        // given
        Long userId = 1L;
        Long questionId = 10L;
        Long answerId = 100L;

        Answer answer = new Answer();
        answer.setAnswerId(answerId);
        answer.setQuestionId(questionId);

        QuestionDto question = new QuestionDto();
        question.setUserId(userId);

        when(answerMapper.findById(answerId)).thenReturn(answer);
        when(questionMapper.findQuestionDetailById(questionId)).thenReturn(question);
        when(answerMapper.existsAcceptedAnswerByQuestionId(questionId)).thenReturn(false);

        // when
        answerService.adoptAnswer(answerId, userId);

        // then
        verify(answerMapper).acceptAnswer(answerId);
    }

    @Test
    @DisplayName("이미 채택된 답변이 있으면 채택할 수 없다 (중복 채택 금지)")
    void adoptAnswer_throws_whenAlreadyAdopted() {
        // given
        Long userId = 1L;
        Long questionId = 10L;
        Long answerId = 100L;

        Answer answer = new Answer();
        answer.setAnswerId(answerId);
        answer.setQuestionId(questionId);

        QuestionDto question = new QuestionDto();
        question.setUserId(userId);

        when(answerMapper.findById(answerId)).thenReturn(answer);
        when(questionMapper.findQuestionDetailById(questionId)).thenReturn(question);
        when(answerMapper.existsAcceptedAnswerByQuestionId(questionId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> answerService.adoptAnswer(answerId, userId))
                .isInstanceOf(ViewException.class);

        verify(answerMapper, never()).acceptAnswer(any());
    }
}
