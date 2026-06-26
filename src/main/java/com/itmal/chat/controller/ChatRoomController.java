package com.itmal.chat.controller;

import com.itmal.auth.domain.CustomUserDetails;
import com.itmal.chat.service.ChatManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat-room")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatManagementService chatManagementService;
    private final SimpMessagingTemplate messagingTemplate;  // ← 추가

    // 채팅방 진입
    @GetMapping("/{chatRoomId}")
    public ResponseEntity<Map<String, Object>> getChatRoom(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(
                chatManagementService.getChatRoomWithMessages(chatRoomId, user.getUserId())
        );
    }

    // 채팅 목록
    @GetMapping("/user")
    public ResponseEntity<List<Map<String, Object>>> getChatRoomsByUser(
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(
                chatManagementService.getChatRoomsWithUnreadCount(user.getUserId())
        );
    }

    @PostMapping("/mark-as-read")
    public ResponseEntity<Void> markAsRead(
            @RequestParam Long chatRoomId,
            @RequestParam Long chatRequestId,
            @AuthenticationPrincipal CustomUserDetails user) {

        Long userId = user.getUserId();

        chatManagementService.markAsRead(chatRoomId, userId, chatRequestId);

        messagingTemplate.convertAndSend(
                "/topic/chat-read/" + chatRoomId,
                Optional.of(Map.of("userId", userId, "readAt", System.currentTimeMillis()))
        );

        return ResponseEntity.ok().build();
    }

    // 채팅방 나가기
    @PostMapping("/{chatRoomId}/leave")
    public ResponseEntity<String> leaveRoom(
            @PathVariable Long chatRoomId,
            @RequestParam Long chatRequestId,
            @AuthenticationPrincipal CustomUserDetails user) {

        chatManagementService.leaveRoom(chatRoomId, user.getUserId(), chatRequestId);
        return ResponseEntity.ok("채팅방을 나갔습니다.");
    }
}