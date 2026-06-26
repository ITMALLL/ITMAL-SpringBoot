package com.itmal.chat.service;

import com.itmal.chat.dto.ChatRequestDto;
import com.itmal.chat.dto.ChatRoomDto;
import com.itmal.chat.mapper.ChatRoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomMapper chatRoomMapper;
    private final ChatRequestService chatRequestService;

    // 채팅방 생성
    public Long createChatRoom(ChatRoomDto chatRoom) {
        chatRoom.setCreatedAt(LocalDateTime.now());
        chatRoom.setUpdatedAt(LocalDateTime.now());
        chatRoomMapper.insertChatRoom(chatRoom);
        return chatRoom.getId();
    }

    // 채팅방 조회
    public ChatRoomDto getChatRoom(Long chatRoomId) {
        return chatRoomMapper.selectById(chatRoomId);
    }

    // chat_request_id로 채팅방 조회(사용 보류)
    public ChatRoomDto getChatRoomByChatRequestId(Long chatRequestId) {
        return chatRoomMapper.selectByChatRequestId(chatRequestId);
    }

    // 마지막 메시지 시간 업데이트
    public void updateLastMessageAt(Long chatRoomId) {
        chatRoomMapper.updateLastMessageAt(chatRoomId, LocalDateTime.now());
    }

    // 사용자 A의 읽음 시간 업데이트
    public void updateLastReadAtA(Long chatRoomId) {
        chatRoomMapper.updateLastReadAtA(chatRoomId, LocalDateTime.now());
    }

    // 사용자 B의 읽음 시간 업데이트
    public void updateLastReadAtB(Long chatRoomId) {
        chatRoomMapper.updateLastReadAtB(chatRoomId, LocalDateTime.now());
    }
    // 채팅방 나가기
    public void leaveRoom(Long chatRoomId, Long userId, Long chatRequestId) {
        if (userId == null || chatRequestId == null || chatRoomId == null) {
            throw new IllegalArgumentException("필수 파라미터가 누락되었습니다.");
        }

        ChatRequestDto chatRequest = chatRequestService.getChatRequest(chatRequestId);
        Boolean isRequester = userId.equals(chatRequest.getRequesterId());

        chatRoomMapper.leaveRoom(chatRoomId, isRequester);
    }

    // A 복구
    public void restoreHiddenA(Long chatRoomId) {
        chatRoomMapper.restoreHiddenA(chatRoomId);
    }

    // B 복구
    public void restoreHiddenB(Long chatRoomId) {
        chatRoomMapper.restoreHiddenB(chatRoomId);
    }
}