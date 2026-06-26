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

    @MessageMapping("/chat")
    public void sendMessage(@Valid @Payload ChatMessageDto message) {
        // 모든 업데이트를 한 번에 (트랜잭션 처리)
        chatMessageService.saveMessageAndUpdateRoom(message);

        // 웹소켓 발송 (트랜잭션 완료 후)
        messagingTemplate.convertAndSend(
                "/topic/user/" + message.getChatRoomId(),
                message
        );
    }
}