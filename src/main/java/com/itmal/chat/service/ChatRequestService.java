package com.itmal.chat.service;

import com.itmal.chat.dto.ChatRequestDto;
import com.itmal.chat.dto.ChatRoomDto;
import com.itmal.chat.mapper.ChatRequestMapper;
import com.itmal.chat.mapper.ChatRoomMapper;
import com.itmal.global.exception.ApiException;
import com.itmal.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRequestService {

    private final ChatRequestMapper chatRequestMapper;
    private final ChatRoomMapper chatRoomMapper;

    // 채팅 요청 생성. 반환값: 기존 방으로 복구된 경우 chatRoomId, 새 요청이면 null
    @Transactional
    public Long createChatRequest(ChatRequestDto chatRequest) {
        ChatRequestDto existing = chatRequestMapper.selectActiveRequestBetweenUsers(
                chatRequest.getRequesterId(), chatRequest.getResponderId());
        if (existing != null) {
            if ("PENDING".equals(existing.getStatus())) {
                throw new ApiException(ErrorCode.DUPLICATE_CHAT_REQUEST);
            }
            // ACCEPTED - 요청자가 나간 방이 있는지 확인
            ChatRoomDto hiddenRoom = chatRoomMapper.findHiddenRoomByRequester(
                    chatRequest.getRequesterId(), chatRequest.getResponderId());
            if (hiddenRoom != null) {
                chatRoomMapper.restoreHiddenA(hiddenRoom.getId());
                return hiddenRoom.getId();
            }
            throw new ApiException(ErrorCode.CHAT_ROOM_ALREADY_EXISTS);
        }
        chatRequest.setStatus("PENDING");
        chatRequest.setCreatedAt(LocalDateTime.now());
        chatRequestMapper.insertChatRequest(chatRequest);
        return null;
    }

    // ✅ 채팅 요청 조회
    public ChatRequestDto getChatRequest(Long chatRequestId) {
        return chatRequestMapper.selectById(chatRequestId);
    }

    // 응답자가 받은 PENDING 요청 목록
    public List<ChatRequestDto> getPendingRequestsForResponder(Long responderId) {
        return chatRequestMapper.selectByResponderIdAndPending(responderId);
    }

    // ✅ 상태 업데이트 (ACCEPTED, REJECTED)
    @Transactional
    public void updateStatus(Long chatRequestId, String status) {
        chatRequestMapper.updateStatus(chatRequestId, status);
    }

    // PENDING 상태일 때만 업데이트 (동시성 안전)
    @Transactional
    public int updateStatusIfPending(Long chatRequestId, String status) {
        return chatRequestMapper.updateStatusIfPending(chatRequestId, status);
    }
}