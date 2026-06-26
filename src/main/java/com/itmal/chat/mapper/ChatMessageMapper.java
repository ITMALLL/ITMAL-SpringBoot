package com.itmal.chat.mapper;

import com.itmal.chat.dto.ChatMessageDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    // 메시지 저장
    void insertChatMessage(ChatMessageDto chatMessage);

    List<ChatMessageDto> selectByChatRoomId(@Param("chatRoomId") Long chatRoomId);

}