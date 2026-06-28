package com.itmal.chat.controller;

import com.itmal.auth.domain.CustomUserDetails;
import com.itmal.chat.dto.ChatMessageDto;
import com.itmal.chat.dto.ChatRequestDto;
import com.itmal.chat.dto.ChatRoomDto;
import com.itmal.chat.service.ChatMessageService;
import com.itmal.chat.service.ChatRequestService;
import com.itmal.chat.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;
    private final ChatRequestService chatRequestService;

    @GetMapping("/chat")
    public String chat() {
        return "chat/chat";
    }

    @MessageMapping("/chat")
    public void sendMessage(@Valid @Payload ChatMessageDto message) {
        chatMessageService.saveMessageAndUpdateRoom(message);

        messagingTemplate.convertAndSend(
                "/topic/user/" + message.getChatRoomId(),
                message
        );

        // 상대방 배지 업데이트 알림
        ChatRoomDto chatRoom = chatRoomService.getChatRoom(message.getChatRoomId());
        ChatRequestDto chatRequest = chatRequestService.getChatRequest(chatRoom.getChatRequestId());
        Long otherUserId = message.getSenderId().equals(chatRequest.getRequesterId())
                ? chatRequest.getResponderId()
                : chatRequest.getRequesterId();

        messagingTemplate.convertAndSend(
                "/topic/unread-count/" + otherUserId,
                (Object) Map.of("chatRoomId", message.getChatRoomId())
        );
    }
    @GetMapping("/api/auth/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails user) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(Map.of(
                "userId", user.getUserId(),
                "username", user.getUsername(),
                "nickname", user.getNickname()
        ));
    }
}