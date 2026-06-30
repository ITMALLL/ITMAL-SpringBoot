package com.itmal.chat.controller;

import com.itmal.auth.domain.CustomUserDetails;
import com.itmal.chat.dto.ChatRequestDto;
import com.itmal.chat.dto.ChatRoomDto;
import com.itmal.chat.service.ChatManagementService;
import com.itmal.chat.service.ChatRequestService;
import com.itmal.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat-room")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatManagementService chatManagementService;
    private final ChatRoomService chatRoomService;
    private final ChatRequestService chatRequestService;
    private final SimpMessagingTemplate messagingTemplate;

    // 채팅 목록
    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> getChatRoomsByUser(
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(
                chatManagementService.getChatListWithUnreadCount(user.getUserId())
        );
    }

    // 채팅방 진입
    @GetMapping("/{chatRoomId}")
    public ResponseEntity<Map<String, Object>> getChatRoom(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal CustomUserDetails user) {

        ChatRoomDto chatRoom = chatRoomService.getChatRoom(chatRoomId);
        ChatRequestDto chatRequest = chatRequestService.getChatRequest(chatRoom.getChatRequestId());

        Long userId = user.getUserId();
        if (!userId.equals(chatRequest.getRequesterId()) && !userId.equals(chatRequest.getResponderId())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(
                chatManagementService.getChatRoomWithMessages(chatRoomId, userId)
        );
    }


    @PostMapping("/mark-as-read")
    public ResponseEntity<Void> markAsRead(
            @RequestParam Long chatRoomId,
            @RequestParam Long chatRequestId,
            @AuthenticationPrincipal CustomUserDetails user) {

        ChatRoomDto chatRoom = chatRoomService.getChatRoom(chatRoomId);
        if (!chatRoom.getChatRequestId().equals(chatRequestId)) {
            return ResponseEntity.status(403).build();
        }

        Long userId = user.getUserId();
        chatManagementService.markAsRead(chatRoomId, userId, chatRequestId);

        messagingTemplate.convertAndSend(
                "/topic/read/" + chatRoomId,
                (Object) Map.of("chatRoomId", chatRoomId)
        );

        return ResponseEntity.ok().build();
    }

    // 채팅방 나가기
    @PostMapping("/{chatRoomId}/leave")
    public ResponseEntity<String> leaveRoom(
            @PathVariable Long chatRoomId,
            @RequestParam Long chatRequestId,
            @AuthenticationPrincipal CustomUserDetails user) {

        ChatRoomDto chatRoom = chatRoomService.getChatRoom(chatRoomId);
        if (!chatRoom.getChatRequestId().equals(chatRequestId)) {
            return ResponseEntity.status(403).build();
        }

        chatManagementService.leaveRoom(chatRoomId, user.getUserId(), chatRequestId);
        return ResponseEntity.ok("채팅방을 나갔습니다.");
    }
}