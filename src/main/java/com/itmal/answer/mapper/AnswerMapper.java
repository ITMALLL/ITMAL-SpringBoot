package com.itmal.answer.mapper;

import com.itmal.answer.domain.Answer;
import com.itmal.answer.domain.AnswerLike;
import com.itmal.answer.dto.AnswerCreateRequest;
import com.itmal.answer.dto.AnswerResponse;
import com.itmal.answer.dto.AnswerUpdateRequest;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AnswerMapper {

    //답변등록
    void insertAnswer(AnswerCreateRequest request);

    //답변 목록 조회
    List<Answer> findByQuestionId(Long questionId);

    //답변 목록 조회 (닉네임 포함, 질문 상세 페이지용)
    List<AnswerResponse> findResponsesByQuestionId(Long questionId);

    //답변 단건 조회
    Answer findById(Long answerId);

    //답변 수정
    void updateAnswer(AnswerUpdateRequest request);

    //답변 삭제
    void deleteAnswer(Long answerId);

    //좋아요 추가
    void insertAnswerLike(AnswerLike answerLike);

    //좋아요 취소
    void deleteAnswerLike(AnswerLike answerLike);

    //좋아요 여부 확인
    AnswerLike findAnswerLike(AnswerLike answerLike);

    //채택
    void acceptAnswer(Long answerId);

    //userId로 조회 (마이페이지용)
    List<Answer> findByUserId(Long userId);
}
