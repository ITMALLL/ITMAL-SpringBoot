package com.itmal.chat.service;

import com.itmal.chat.dto.ChatRequestDto;
import com.itmal.chat.mapper.ChatRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatRequestService {

    private final ChatRequestMapper chatRequestMapper;

    // 채팅 요청 생성
    public void createChatRequest(ChatRequestDto chatRequest) {
        chatRequest.setStatus("PENDING");
        chatRequest.setCreatedAt(LocalDateTime.now());
        chatRequestMapper.insertChatRequest(chatRequest);
    }

    // 채팅 요청 수락
    public void acceptChatRequest(Long chatRequestId) {
        chatRequestMapper.updateStatus(chatRequestId, "ACCEPTED");
    }

    // 채팅 요청 거절
    public void rejectChatRequest(Long chatRequestId) {
        chatRequestMapper.updateStatus(chatRequestId, "REJECTED");
    }

    // 상세 조회
    public ChatRequestDto getChatRequest(Long chatRequestId) {
        return chatRequestMapper.selectById(chatRequestId);
    }
}