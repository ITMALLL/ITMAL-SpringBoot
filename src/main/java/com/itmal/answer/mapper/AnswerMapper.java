package com.itmal.answer.mapper;

import com.itmal.answer.domain.Answer;
import com.itmal.answer.domain.AnswerLike;
import com.itmal.answer.dto.AnswerCreateRequest;
import com.itmal.answer.dto.AnswerResponse;
import com.itmal.answer.dto.AnswerUpdateRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnswerMapper {

    //답변등록
    void insertAnswer(AnswerCreateRequest request);

    //답변 목록 조회
    List<Answer> findByQuestionId(Long questionId);

    //답변 목록 조회 (닉네임 포함, 질문 상세 페이지용) — userId는 좋아요 여부 판정용(비로그인 null)
    List<AnswerResponse> findResponsesByQuestionId(@Param("questionId") Long questionId,
                                                   @Param("userId") Long userId);

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

    //좋아요 개수 조회
    int countAnswerLikes(Long answerId);

    //채택
    void acceptAnswer(Long answerId);

    //채택 가능 여부 확인용 (해당 질문에 이미 채택된 답변이 있는지)
    boolean existsAcceptedAnswerByQuestionId(Long questionId);

    //채택 동시성 제어용: 해당 질문의 답변 행을 잠그고 재검증하기 위한 락
    List<Long> lockAnswersByQuestionId(Long questionId);

    //userId로 조회 (마이페이지용)
    List<Answer> findByUserId(Long userId);
}
