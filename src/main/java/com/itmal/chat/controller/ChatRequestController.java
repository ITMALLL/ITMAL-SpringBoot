package com.itmal.chat.controller;

import com.itmal.auth.domain.CustomUserDetails;
import com.itmal.chat.dto.ChatRequestDto;
import com.itmal.chat.service.ChatManagementService;
import com.itmal.chat.service.ChatRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat-request")
@RequiredArgsConstructor
public class ChatRequestController {

    private final ChatRequestService chatRequestService;
    private final ChatManagementService chatManagementService;
    private final SimpMessagingTemplate messagingTemplate;  // ← 추가

    // 채팅 요청 생성
    @PostMapping
    public ResponseEntity<String> createChatRequest(
            @Valid @RequestBody ChatRequestDto chatRequest) {
        chatRequestService.createChatRequest(chatRequest);
        return ResponseEntity.ok("채팅 요청이 전송되었습니다.");
    }

    // 채팅 요청 거절
    @PutMapping("/{chatRequestId}/reject")
    public ResponseEntity<String> rejectChatRequest(
            @PathVariable Long chatRequestId) {

        chatManagementService.rejectChatRequest(chatRequestId);
        return ResponseEntity.ok("채팅 요청을 거절했습니다.");
    }

    // 채팅 요청 상세 조회 (지금 안쓰임)
    @GetMapping("/{chatRequestId}")
    public ResponseEntity<ChatRequestDto> getChatRequest(
            @PathVariable Long chatRequestId) {
        return ResponseEntity.ok(chatRequestService.getChatRequest(chatRequestId));
    }

    // 응답자가 받은 PENDING 요청 목록
    @GetMapping("/pending")
    public ResponseEntity<List<ChatRequestDto>> getPendingRequests(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(
                chatRequestService.getPendingRequestsForResponder(user.getUserId())
        );
    }
    @PutMapping("/{chatRequestId}/accept")
    public ResponseEntity<Map<String, Long>> acceptChatRequest(
            @PathVariable Long chatRequestId,
            @AuthenticationPrincipal CustomUserDetails user) {

        ChatRequestDto chatRequest = chatRequestService.getChatRequest(chatRequestId);

        if (!chatRequest.getResponderId().equals(user.getUserId())) {
            return ResponseEntity.status(403).build();
        }

        Long chatRoomId = chatManagementService.acceptChatRequest(chatRequestId);

        messagingTemplate.convertAndSend(
                "/topic/chat-rooms/" + chatRequest.getRequesterId(),
                Optional.of(new HashMap<String, Object>() {{
                    put("type", "ROOM_CREATED");
                    put("chatRoomId", chatRoomId);
                }})
        );

        Map<String, Long> response = new HashMap<>();
        response.put("chatRoomId", chatRoomId);
        return ResponseEntity.ok(response);
    }
}