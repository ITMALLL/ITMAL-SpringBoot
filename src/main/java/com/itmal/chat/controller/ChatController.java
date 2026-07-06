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
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;
    private final ChatRequestService chatRequestService;

    @GetMapping("/chat")
    public String chat(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("currentUserId", userDetails.getUserId());
        return "chat/chat";
    }

    @MessageMapping("/chat/typing/{otherUserId}")
    public void typing(@DestinationVariable Long otherUserId, Principal principal) {
        messagingTemplate.convertAndSend(
                "/topic/typing/" + otherUserId,
                (Object) Map.of("senderId", ((CustomUserDetails) ((Authentication) principal).getPrincipal()).getUserId())
        );
    }

    @MessageMapping("/chat")
    public void sendMessage(@Valid @Payload ChatMessageDto message, Principal principal) {
        CustomUserDetails userDetails = (CustomUserDetails) ((Authentication) principal).getPrincipal();
        Long authenticatedUserId = userDetails.getUserId();

        // 채팅방 및 참여자 검증
        ChatRoomDto chatRoom = chatRoomService.getChatRoom(message.getChatRoomId());
        ChatRequestDto chatRequest = chatRequestService.getChatRequest(chatRoom.getChatRequestId());

        boolean isParticipant = authenticatedUserId.equals(chatRequest.getRequesterId())
                || authenticatedUserId.equals(chatRequest.getResponderId());
        if (!isParticipant) return;

        // 클라이언트 senderId 무시하고 인증된 사용자로 덮어쓰기
        message.setSenderId(authenticatedUserId);

        chatMessageService.saveMessageAndUpdateRoom(message);

        messagingTemplate.convertAndSend(
                "/topic/user/" + message.getChatRoomId(),
                message
        );

        // 상대방 배지 업데이트 알림
        Long otherUserId = authenticatedUserId.equals(chatRequest.getRequesterId())
                ? chatRequest.getResponderId()
                : chatRequest.getRequesterId();

        messagingTemplate.convertAndSend(
                "/topic/unread-count/" + otherUserId,
                (Object) Map.of("chatRoomId", message.getChatRoomId())
        );
    }
}