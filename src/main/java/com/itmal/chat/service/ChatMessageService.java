package com.itmal.chat.service;

import com.itmal.chat.dto.ChatMessageDto;
import com.itmal.chat.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageMapper chatMessageMapper;

    // 메시지 저장
    public void saveChatMessage(ChatMessageDto chatMessage) {
        chatMessage.setCreatedAt(LocalDateTime.now());
        chatMessage.setIsRead(false);
        chatMessageMapper.insertChatMessage(chatMessage);
    }

    // 채팅방의 메시지 목록 조회
    public List<ChatMessageDto> getChatMessagesByChatRoom(Long chatRoomId) {
        return chatMessageMapper.selectByChatRoomId(chatRoomId);
    }

}