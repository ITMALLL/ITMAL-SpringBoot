package com.itmal.chat.controller;

import com.itmal.chat.dto.ChatMessageDto;
import com.itmal.chat.dto.ChatRequestDto;
import com.itmal.chat.dto.ChatRoomDto;
import com.itmal.chat.service.ChatMessageService;
import com.itmal.chat.service.ChatRequestService;
import com.itmal.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat-room")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatRequestService chatRequestService;
    private final ChatMessageService chatMessageService;

    @GetMapping("/{chatRoomId}")
    public ResponseEntity<Map<String, Object>> getChatRoom(
            @PathVariable Long chatRoomId,
            @RequestParam Long userId) {

        ChatRoomDto chatRoom = chatRoomService.getChatRoom(chatRoomId);
        ChatRequestDto chatRequest = chatRequestService.getChatRequest(chatRoom.getChatRequestId());

        // 읽음 처리
        if (userId.equals(chatRequest.getRequesterId())) {
            chatRoomService.updateLastReadAtA(chatRoomId);
        } else {
            chatRoomService.updateLastReadAtB(chatRoomId);
        }

        // 메시지 목록 조회
        List<ChatMessageDto> messages = chatMessageService.getChatMessagesByChatRoom(chatRoomId);

        // 응답 객체 생성
        Map<String, Object> response = new HashMap<>();
        response.put("chatRoom", chatRoom);
        response.put("messages", messages);

        return ResponseEntity.ok(response);
    }

    // 채팅방 나가기
    @PostMapping("/{chatRoomId}/leave")
    public ResponseEntity<String> leaveRoom(
            @PathVariable Long chatRoomId,
            @RequestParam Long userId,
            @RequestParam Long chatRequestId) {
        chatRoomService.leaveRoom(chatRoomId, userId, chatRequestId);
        return ResponseEntity.ok("채팅방을 나갔습니다.");
    }

}