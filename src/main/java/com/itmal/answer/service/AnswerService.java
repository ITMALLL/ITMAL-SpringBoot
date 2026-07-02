package com.itmal.answer.service;

import com.itmal.answer.domain.Answer;
import com.itmal.answer.dto.AnswerCreateRequest;
import com.itmal.answer.dto.AnswerLikeResponseDto;
import com.itmal.answer.dto.AnswerResponse;
import com.itmal.answer.dto.AnswerUpdateRequest;

import java.util.List;

public interface AnswerService {
    void createAnswer(AnswerCreateRequest request, Long userId);
    List<Answer> getAnswerByQuestionId(Long questionId);
    List<AnswerResponse> getAnswerResponsesByQuestionId(Long questionId);
    Answer getAnswer(Long answerId);
    void updateAnswer(Long answerId, AnswerUpdateRequest request, Long userId);
    void deleteAnswer(Long answerId, Long userId);
    AnswerLikeResponseDto toggleLike(Long answerId, Long userId);
    void adoptAnswer(Long answerId, Long userId);
    List<Answer> getAnswerByUserId(Long userId);
}
