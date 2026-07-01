package com.itmal.answer.service;

import com.itmal.answer.domain.Answer;
import com.itmal.answer.domain.AnswerLike;
import com.itmal.answer.dto.AnswerCreateRequest;
import com.itmal.answer.dto.AnswerResponse;
import com.itmal.answer.dto.AnswerUpdateRequest;
import com.itmal.answer.mapper.AnswerMapper;
import com.itmal.global.exception.BusinessException;
import com.itmal.global.exception.ErrorCode;
import com.itmal.notification.service.NotificationService;
import com.itmal.question.dto.QuestionDto;
import com.itmal.question.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnswerServiceImpl implements AnswerService {

    private final AnswerMapper answerMapper;
    private final NotificationService notificationService;
    private final QuestionMapper questionMapper;

    // 답변 등록
    @Override
    public void createAnswer(AnswerCreateRequest request, Long userId) {
        // userId는 폼이 아닌 로그인 세션에서 가져와서 세팅
        request.setUserId(userId);
        answerMapper.insertAnswer(request);
        notificationService.createAnswerNotification(request.getQuestionId(), request.getAnswerId());
    }

    // 답변 목록 조회
    @Override
    public List<Answer> getAnswerByQuestionId(Long questionId) {
        return answerMapper.findByQuestionId(questionId);
    }

    // 답변 목록 조회 (닉네임 포함)
    @Override
    public List<AnswerResponse> getAnswerResponsesByQuestionId(Long questionId) {
        return answerMapper.findResponsesByQuestionId(questionId);
    }

    // 답변 단건 조회
    @Override
    public Answer getAnswer(Long answerId) {
        Answer answer = answerMapper.findById(answerId);
        if (answer == null) {
            throw new BusinessException(ErrorCode.ANSWER_NOT_FOUND);
        }
        return answer;
    }

    // 답변 수정 (본인만 가능)
    @Override
    public void updateAnswer(Long answerId, AnswerUpdateRequest request, Long userId) {
        Answer answer = getAnswer(answerId);
        if (!answer.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        request.setAnswerId(answerId);
        answerMapper.updateAnswer(request);
    }

    // 답변 삭제 (본인만 가능)
    @Override
    public void deleteAnswer(Long answerId, Long userId) {
        Answer answer = getAnswer(answerId);
        if (!answer.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        answerMapper.deleteAnswer(answerId);
    }

    // 좋아요 토글 (있으면 취소, 없으면 추가)
    @Override
    public void toggleLike(Long answerId, Long userId) {
        getAnswer(answerId); // 삭제된 답변 체크
        AnswerLike answerLike = new AnswerLike();
        answerLike.setAnswerId(answerId);
        answerLike.setUserId(userId);

        AnswerLike existing = answerMapper.findAnswerLike(answerLike);
        if (existing != null) {
            answerMapper.deleteAnswerLike(answerLike);
        } else {
            try {
                answerMapper.insertAnswerLike(answerLike);
            } catch (DuplicateKeyException e) {
                // 동시 요청으로 중복 좋아요 insert 시 무시
            }
        }
    }

    // 마이페이지 내 답변 목록 조회
    @Override
    public List<Answer> getAnswerByUserId(Long userId) {
        return answerMapper.findByUserId(userId);
    }

    // 채택
    @Override
    public void adoptAnswer(Long answerId, Long userId) {
        Answer answer = getAnswer(answerId);
        QuestionDto question = questionMapper.findQuestionDetailById(answer.getQuestionId());
        if (question == null || !question.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        answerMapper.acceptAnswer(answerId);
    }
}
