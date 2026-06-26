package com.itmal.chat.controller;

import com.itmal.chat.dto.ChatMessageDto;
import com.itmal.chat.service.ChatMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;

    @GetMapping("/chat")
    public String chat() {
        return "chat/chat";
    }

    @MessageMapping("/chat")
    public void sendMessage(@Valid @Payload ChatMessageDto message) {
        // 메시지 저장 + 마지막 메시지 시간 업데이트
        chatMessageService.saveMessageAndUpdateRoom(message);

        // STOMP로 전송
        messagingTemplate.convertAndSend(
                "/topic/user/" + message.getChatRoomId(),
                message
        );
    }
}