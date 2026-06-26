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
        // 1. 메시지 저장
        saveChatMessage(message);

        // 2. 채팅방 정보 조회
        ChatRoomDto chatRoom = chatRoomService.getChatRoom(message.getChatRoomId());

        // 3. 채팅방 업데이트
        chatRoomService.updateLastMessageAt(message.getChatRoomId());

        // 4. A/B 판단
        ChatRequestDto chatRequest = chatRequestService.getChatRequest(chatRoom.getChatRequestId());

        // 5. 나간 사용자 복구
        if (message.getSenderId().equals(chatRequest.getRequesterId())) {
            chatRoomService.restoreHiddenB(message.getChatRoomId());
        } else {
            chatRoomService.restoreHiddenA(message.getChatRoomId());
        }
    }

    public void saveChatMessage(ChatMessageDto chatMessage) {
        chatMessage.setCreatedAt(LocalDateTime.now());
        chatMessage.setIsRead(false);
        chatMessageMapper.insertChatMessage(chatMessage);
    }

    public List<ChatMessageDto> getChatMessagesByChatRoom(Long chatRoomId) {
        return chatMessageMapper.selectByChatRoomId(chatRoomId);
    }
    // 채팅방의 메시지 조회 (last_read_at 기준으로 읽음 처리)
    public List<ChatMessageDto> getChatMessagesByChatRoomWithReadStatus(Long chatRoomId, LocalDateTime lastReadAt) {
        List<ChatMessageDto> messages = chatMessageMapper.selectByChatRoomId(chatRoomId);

        // lastReadAt 이전의 메시지는 읽음 처리
        if (lastReadAt != null) {
            messages.forEach(msg -> {
                if (msg.getCreatedAt().isBefore(lastReadAt) || msg.getCreatedAt().isEqual(lastReadAt)) {
                    msg.setIsRead(true);
                }
            });
        }

        return messages;
    }
}