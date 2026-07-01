package com.itmal.answer.service;

import com.itmal.answer.domain.Answer;
import com.itmal.answer.mapper.AnswerMapper;
import com.itmal.notification.service.NotificationService;
import com.itmal.question.mapper.QuestionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
}
