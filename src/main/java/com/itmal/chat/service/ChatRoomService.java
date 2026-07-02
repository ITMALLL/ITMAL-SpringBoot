package com.itmal.chat.service;

import com.itmal.chat.dto.ChatRoomDto;
import com.itmal.chat.mapper.ChatRequestMapper;
import com.itmal.chat.mapper.ChatRoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomMapper chatRoomMapper;
    private final ChatRequestMapper chatRequestMapper;

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

    // chatRequestId로 채팅방 조회
    public ChatRoomDto getChatRoomByChatRequestId(Long chatRequestId) {
        return chatRoomMapper.selectByChatRequestId(chatRequestId);
    }

    // 사용자의 모든 채팅방 조회
    public List<ChatRoomDto> getChatRoomsByUser(Long userId) {
        return chatRoomMapper.selectByUserId(userId);
    }

    // 마지막 메시지 시간 업데이트
    @Transactional
    public void updateLastMessageAt(Long chatRoomId) {
        chatRoomMapper.updateLastMessageAt(chatRoomId, LocalDateTime.now());
    }

    // 읽음 시각 업데이트 (A: 요청자, B: 응답자)
    @Transactional
    public void updateLastReadAt(Long chatRoomId, Boolean isRequester) {
        if (isRequester) {
            chatRoomMapper.updateLastReadAtA(chatRoomId, LocalDateTime.now());
        } else {
            chatRoomMapper.updateLastReadAtB(chatRoomId, LocalDateTime.now());
        }
    }

    // 채팅방 나가기
    @Transactional
    public void leaveRoom(Long chatRoomId, Boolean isRequester) {
        if (isRequester == null || chatRoomId == null) {
            throw new IllegalArgumentException("필수 파라미터가 누락되었습니다.");
        }
        chatRoomMapper.leaveRoom(chatRoomId, isRequester);
        if (chatRoomMapper.isBothLeft(chatRoomId)) {
            ChatRoomDto chatRoom = chatRoomMapper.selectById(chatRoomId);
            if (chatRoom == null) return;
            chatRoomMapper.deleteRoom(chatRoomId);
            chatRequestMapper.deleteById(chatRoom.getChatRequestId());
        }
    }

    // 숨겨진 채팅방 복구 (A)
    @Transactional
    public void restoreHiddenA(Long chatRoomId) {
        chatRoomMapper.restoreHiddenA(chatRoomId);
    }

    // 숨겨진 채팅방 복구 (B)
    @Transactional
    public void restoreHiddenB(Long chatRoomId) {
        chatRoomMapper.restoreHiddenB(chatRoomId);
    }
}