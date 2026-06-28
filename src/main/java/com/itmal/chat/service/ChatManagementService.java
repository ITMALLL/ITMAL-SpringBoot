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

    public List<Map<String, Object>> getChatListWithUnreadCount(Long userId) {
        List<ChatRoomDto> rooms = chatRoomService.getChatRoomsByUser(userId);
        List<Map<String, Object>> response = new ArrayList<>();

        for (ChatRoomDto room : rooms) {
            ChatRequestDto chatRequest = chatRequestService.getChatRequest(room.getChatRequestId());
            boolean isRequester = userId.equals(chatRequest.getRequesterId());

            if (isRequester && Boolean.TRUE.equals(room.getHiddenByA())) {
                continue;
            }
            if (!isRequester && Boolean.TRUE.equals(room.getHiddenByB())) {
                continue;
            }

            Long otherUserId = isRequester
                    ? chatRequest.getResponderId()
                    : chatRequest.getRequesterId();

            long unreadCount = getUnreadCount(room.getId(), userId);

            ChatMessageDto lastMessage = chatMessageService.getLastMessageByChatRoomAndUser(room.getId(), userId);

            Map<String, Object> roomInfo = new HashMap<>();
            roomInfo.put("id", room.getId());
            roomInfo.put("chatRequestId", room.getChatRequestId());
            roomInfo.put("lastMessageAt", room.getLastMessageAt());
            roomInfo.put("unreadCount", unreadCount);
            roomInfo.put("otherUserId", otherUserId);

            if (lastMessage != null) {
                roomInfo.put("lastMessage", lastMessage.getContent());
            } else {
                roomInfo.put("lastMessage", ". . .");
            }

            response.add(roomInfo);
        }

        return response;
    }
    // 채팅방 진입 (메시지 + 읽음 상태 로드)
    public Map<String, Object> getChatRoomWithMessages(Long chatRoomId, Long userId) {
        ChatRoomDto chatRoom = chatRoomService.getChatRoom(chatRoomId);
        ChatRequestDto chatRequest = chatRequestService.getChatRequest(chatRoom.getChatRequestId());
        List<ChatMessageDto> messages = chatMessageService.getChatMessagesByChatRoomAndUser(chatRoomId, userId);

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
        Long otherUserId = isRequester
                ? chatRequest.getResponderId()
                : chatRequest.getRequesterId();
        System.out.println(otherUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("chatRoom", chatRoom);
        response.put("messages", messages);
        response.put("responderInfo", chatRequest);
        response.put("otherUserId", otherUserId);

        return response;
    }

    //안읽은 메세지 개수
    public long getUnreadCount(Long chatRoomId, Long userId) {
        ChatRoomDto room = chatRoomService.getChatRoom(chatRoomId);
        ChatRequestDto chatRequest = chatRequestService.getChatRequest(room.getChatRequestId());

        List<ChatMessageDto> messages = chatMessageService.getChatMessagesByChatRoomAndUser(chatRoomId, userId);

        boolean isRequester = userId.equals(chatRequest.getRequesterId());

        LocalDateTime lastReadAt = isRequester ? room.getLastReadAtA() : room.getLastReadAtB();

        return messages.stream()
                .filter(msg -> !Objects.equals(msg.getSenderId(), userId)
                        && (lastReadAt == null || msg.getCreatedAt().isAfter(lastReadAt)))
                .count();
    }

    // 채팅방 나가기
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
    // 읽음 처리 (비즈니스 로직만)
    @Transactional
    public void markAsRead(Long chatRoomId, Long userId, Long chatRequestId) {
        ChatRequestDto chatRequest = chatRequestService.getChatRequest(chatRequestId);
        Boolean isRequester = userId.equals(chatRequest.getRequesterId());

        // DB 업데이트
        chatRoomService.updateLastReadAt(chatRoomId, userId, isRequester);
    }
}