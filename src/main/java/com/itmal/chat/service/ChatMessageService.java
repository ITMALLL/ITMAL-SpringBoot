package com.itmal.chat.service;

import com.itmal.chat.dto.ChatMessageDto;
import com.itmal.chat.dto.ChatRequestDto;
import com.itmal.chat.dto.ChatRoomDto;
import com.itmal.chat.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageMapper chatMessageMapper;
    private final ChatRoomService chatRoomService;
    private final ChatRequestService chatRequestService;

    // 메시지 저장 + 채팅방 업데이트 (원자적 처리)
    @Transactional
    public void saveMessageAndUpdateRoom(ChatMessageDto message) {
        saveChatMessage(message);

        // 채팅방 정보 조회
        ChatRoomDto chatRoom = chatRoomService.getChatRoom(message.getChatRoomId());

        // 채팅방 업데이트
        chatRoomService.updateLastMessageAt(message.getChatRoomId());

        // A/B 판단
        ChatRequestDto chatRequest = chatRequestService.getChatRequest(chatRoom.getChatRequestId());

        // 나간 사용자 복구
        if (message.getSenderId().equals(chatRequest.getRequesterId())) {
            chatRoomService.restoreHiddenB(message.getChatRoomId());
        } else {
            chatRoomService.restoreHiddenA(message.getChatRoomId());
        }
    }
    public ChatMessageDto getLastMessageByChatRoomAndUser(Long chatRoomId, Long userId) {
        return chatMessageMapper.selectLastMessageByChatRoomAndUser(chatRoomId, userId);
    }
    public void saveChatMessage(ChatMessageDto chatMessage) {
        chatMessage.setCreatedAt(LocalDateTime.now());
        chatMessage.setIsRead(false);
        chatMessageMapper.insertChatMessage(chatMessage);
    }
    public List<ChatMessageDto> getChatMessagesByChatRoomAndUser(Long chatRoomId, Long userId) {
        return chatMessageMapper.selectByChatRoomIdAndUser(chatRoomId, userId);
    }
}