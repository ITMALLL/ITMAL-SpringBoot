package com.itmal.chat.controller;

import com.itmal.chat.dto.ChatMessageDto;
import com.itmal.chat.dto.ChatRequestDto;
import com.itmal.chat.dto.ChatRoomDto;
import com.itmal.chat.service.ChatMessageService;
import com.itmal.chat.service.ChatRoomService;
import com.itmal.chat.service.ChatRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;
    private final ChatRequestService chatRequestService;

    @MessageMapping("/chat")
    public void sendMessage(@Valid @Payload ChatMessageDto message) {
        // DB에 메시지 저장
        chatMessageService.saveChatMessage(message);

        // 채팅방 정보 조회
        ChatRoomDto chatRoom = chatRoomService.getChatRoom(message.getChatRoomId());

        // 채팅방 업데이트
        chatRoomService.updateLastMessageAt(message.getChatRoomId());

        // 채팅 요청 정보로 A, B 판단
        ChatRequestDto chatRequest = chatRequestService.getChatRequest(chatRoom.getChatRequestId());

        // 메시지 보낸 사람이 A라면 B를 복구, B라면 A를 복구
        if (message.getSenderId().equals(chatRequest.getRequesterId())) {
            // 보낸 사람이 A(requester) → B 복구
            chatRoomService.restoreHiddenB(message.getChatRoomId());
        } else {
            // 보낸 사람이 B(responder) → A 복구
            chatRoomService.restoreHiddenA(message.getChatRoomId());
        }

        // WebSocket으로 수신자에게 메시지 발송
        messagingTemplate.convertAndSend(
                "/topic/user/" + message.getChatRoomId(),
                message
        );
    }
}