package com.itmal.chat.mapper;

import com.itmal.chat.dto.ChatRequestDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


@Mapper
public interface ChatRequestMapper {

    // 채팅 요청 생성
    void insertChatRequest(ChatRequestDto chatRequest);

    // ID로 조회
    ChatRequestDto selectById(@Param("chatRequestId") Long chatRequestId);

    // 상태 업데이트 (수락/거절)
    void updateStatus(@Param("chatRequestId") Long chatRequestId,
                      @Param("status") String status);
}

