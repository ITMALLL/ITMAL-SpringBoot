package com.itmal.chat.service;

import com.itmal.chat.dto.ChatMessageDto;
import com.itmal.chat.dto.ChatRequestDto;
import com.itmal.chat.dto.ChatRoomDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatManagementService {

    private final ChatRequestService chatRequestService;
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    // 채팅방 진입 (메시지 + 읽음 상태)
    public Map<String, Object> getChatRoomWithMessages(Long chatRoomId, Long userId) {
        ChatRoomDto chatRoom = chatRoomService.getChatRoom(chatRoomId);
        ChatRequestDto chatRequest = chatRequestService.getChatRequest(chatRoom.getChatRequestId());
        List<ChatMessageDto> messages = chatMessageService.getChatMessagesByChatRoom(chatRoomId);

        // 읽음 상태 계산 (상대방이 읽었는가)
        boolean isRequester = userId.equals(chatRequest.getRequesterId());
        LocalDateTime lastReadAt = isRequester ? chatRoom.getLastReadAtB() : chatRoom.getLastReadAtA();

        // 메시지별 isRead 계산
        messages.forEach(msg -> {
            if (msg.getSenderId().equals(userId)) {
                // 내가 보낸 메시지: 상대방이 읽었는가?
                msg.setIsRead(lastReadAt != null && msg.getCreatedAt().isBefore(lastReadAt));
            } else {
                // 상대방이 보낸 메시지: 이미 읽음 처리됨
                msg.setIsRead(true);
            }
        });

        Map<String, Object> response = new HashMap<>();
        response.put("chatRoom", chatRoom);
        response.put("messages", messages);
        response.put("responderInfo", chatRequest);

        return response;
    }

    public List<Map<String, Object>> getChatRoomsWithUnreadCount(Long userId) {
        List<ChatRoomDto> rooms = chatRoomService.getChatRoomsByUser(userId);
        List<Map<String, Object>> response = new ArrayList<>();

        for (ChatRoomDto room : rooms) {
            ChatRequestDto chatRequest = chatRequestService.getChatRequest(room.getChatRequestId());
            List<ChatMessageDto> messages = chatMessageService.getChatMessagesByChatRoom(room.getId());

            // ✅ 읽지 않은 메시지 수 계산 (수정됨!)
            long unreadCount = 0;
            if (userId.equals(chatRequest.getRequesterId())) {
                LocalDateTime lastReadAt = room.getLastReadAtA();
                unreadCount = messages.stream()
                        .filter(msg -> msg.getSenderId() != userId  // 상대방 메시지만!
                                && (lastReadAt == null || msg.getCreatedAt().isAfter(lastReadAt)))
                        .count();
            } else {
                LocalDateTime lastReadAt = room.getLastReadAtB();
                unreadCount = messages.stream()
                        .filter(msg -> msg.getSenderId() != userId  // 상대방 메시지만!
                                && (lastReadAt == null || msg.getCreatedAt().isAfter(lastReadAt)))
                        .count();
            }

            Map<String, Object> roomInfo = new HashMap<>();
            roomInfo.put("id", room.getId());
            roomInfo.put("chatRequestId", room.getChatRequestId());
            roomInfo.put("lastMessageAt", room.getLastMessageAt());
            roomInfo.put("unreadCount", unreadCount);

            response.add(roomInfo);
        }

        return response;
    }



    // ✅ 채팅방 나가기
    @Transactional
    public void leaveRoom(Long chatRoomId, Long userId, Long chatRequestId) {
        if (userId == null || chatRequestId == null || chatRoomId == null) {
            throw new IllegalArgumentException("필수 파라미터가 누락되었습니다.");
        }

        ChatRequestDto chatRequest = chatRequestService.getChatRequest(chatRequestId);
        Boolean isRequester = userId.equals(chatRequest.getRequesterId());

        chatRoomService.leaveRoom(chatRoomId, isRequester);
    }

    // 채팅 요청 수락 서비스 로직(트랜잭션 처리)
    @Transactional
    public Long acceptChatRequest(Long chatRequestId) {
        ChatRequestDto chatRequest = chatRequestService.getChatRequest(chatRequestId);

        if ("ACCEPTED".equals(chatRequest.getStatus())) {
            ChatRoomDto existingRoom = chatRoomService.getChatRoomByChatRequestId(chatRequestId);
            return existingRoom.getId();
        }

        if (!"PENDING".equals(chatRequest.getStatus())) {
            throw new IllegalStateException("채팅 요청 상태가 올바르지 않습니다.");
        }

        ChatRoomDto chatRoom = new ChatRoomDto();
        chatRoom.setChatRequestId(chatRequestId);
        Long chatRoomId = chatRoomService.createChatRoom(chatRoom);

        chatRequestService.updateStatus(chatRequestId, "ACCEPTED");

        return chatRoomId;
    }

    // 채팅 요청 거절(상태만 변경하면 됨)
    @Transactional
    public void rejectChatRequest(Long chatRequestId) {
        chatRequestService.updateStatus(chatRequestId, "REJECTED");
    }
    // ✅ 읽음 처리 (비즈니스 로직만)
    @Transactional
    public void markAsRead(Long chatRoomId, Long userId, Long chatRequestId) {
        ChatRequestDto chatRequest = chatRequestService.getChatRequest(chatRequestId);
        Boolean isRequester = userId.equals(chatRequest.getRequesterId());

        // DB 업데이트
        chatRoomService.updateLastReadAt(chatRoomId, userId, isRequester);
    }
}