package com.itmal.chat.controller;

import com.itmal.chat.dto.ChatRequestDto;
import com.itmal.chat.dto.ChatRoomDto;
import com.itmal.chat.service.ChatRequestService;
import com.itmal.chat.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat-request")
@RequiredArgsConstructor
public class ChatRequestController {

    private final ChatRequestService chatRequestService;
    private final ChatRoomService chatRoomService;

    // 채팅 요청 생성
    @PostMapping
    public ResponseEntity<String> createChatRequest(@Valid @RequestBody ChatRequestDto chatRequest) {
        chatRequestService.createChatRequest(chatRequest);
        return ResponseEntity.ok("채팅 요청이 전송되었습니다.");
    }

    // 채팅 요청 수락
    @PutMapping("/{chatRequestId}/accept")
    public ResponseEntity<String> acceptChatRequest(@PathVariable Long chatRequestId) {
        ChatRequestDto chatRequest = chatRequestService.getChatRequest(chatRequestId);

        // 채팅방 생성
        ChatRoomDto chatRoom = new ChatRoomDto();
        chatRoom.setChatRequestId(chatRequestId);
        chatRoom.setChatTitle("Chat with " + chatRequest.getRequesterId());
        chatRoomService.createChatRoom(chatRoom);

        // 요청 상태 수락으로 변경
        chatRequestService.acceptChatRequest(chatRequestId);

        return ResponseEntity.ok("채팅 요청을 수락했습니다.");
    }

    // 채팅 요청 거절
    @PutMapping("/{chatRequestId}/reject")
    public ResponseEntity<String> rejectChatRequest(@PathVariable Long chatRequestId) {
        chatRequestService.rejectChatRequest(chatRequestId);
        return ResponseEntity.ok("채팅 요청을 거절했습니다.");
    }

    // 채팅 요청 상세 조회
    @GetMapping("/{chatRequestId}")
    public ResponseEntity<ChatRequestDto> getChatRequest(@PathVariable Long chatRequestId) {
        return ResponseEntity.ok(chatRequestService.getChatRequest(chatRequestId));
    }
}