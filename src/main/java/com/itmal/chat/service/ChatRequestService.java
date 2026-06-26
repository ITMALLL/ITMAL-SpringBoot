package com.itmal.chat.service;

import com.itmal.chat.dto.ChatRequestDto;
import com.itmal.chat.mapper.ChatRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRequestService {

    private final ChatRequestMapper chatRequestMapper;

    // ✅ 채팅 요청 생성
    public void createChatRequest(ChatRequestDto chatRequest) {
        chatRequest.setStatus("PENDING");
        chatRequest.setCreatedAt(LocalDateTime.now());
        chatRequestMapper.insertChatRequest(chatRequest);
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
}