package com.itmal.chat.mapper;

import com.itmal.chat.dto.ChatRequestDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface ChatRequestMapper {

    // 채팅 요청 생성
    void insertChatRequest(ChatRequestDto chatRequest);

    // ID로 조회
    ChatRequestDto selectById(@Param("chatRequestId") Long chatRequestId);

    // 상태 업데이트 (수락/거절)
    void updateStatus(@Param("chatRequestId") Long chatRequestId,
                      @Param("status") String status);

    // PENDING 상태일 때만 업데이트 (동시성 안전)
    int updateStatusIfPending(@Param("chatRequestId") Long chatRequestId,
                              @Param("status") String status);

    // 응답자 ID로 PENDING 상태 요청 조회
    List<ChatRequestDto> selectByResponderIdAndPending(@Param("responderId") Long responderId);

    // 두 유저 간 PENDING 또는 ACCEPTED 요청 조회
    ChatRequestDto selectActiveRequestBetweenUsers(@Param("requesterId") Long requesterId,
                                                   @Param("responderId") Long responderId);
}

