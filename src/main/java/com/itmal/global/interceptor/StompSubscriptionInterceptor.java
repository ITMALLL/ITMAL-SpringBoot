package com.itmal.global.interceptor;

import com.itmal.auth.domain.CustomUserDetails;
import com.itmal.chat.dto.ChatRequestDto;
import com.itmal.chat.dto.ChatRoomDto;
import com.itmal.chat.service.ChatRequestService;
import com.itmal.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;

// 채팅 관련 topic 구독 시, 요청자 본인 소유(userId) 또는 참여 중인 채팅방(chatRoomId)인지 검증
@Component
@RequiredArgsConstructor
public class StompSubscriptionInterceptor implements ChannelInterceptor {

    private static final String[] OWN_USER_ID_PREFIXES = {
            "/topic/chat-rooms/", "/topic/unread-count/", "/topic/typing/"
    };
    private static final String[] CHAT_ROOM_PREFIXES = {
            "/topic/user/", "/topic/read/"
    };

    private final ChatRoomService chatRoomService;
    private final ChatRequestService chatRequestService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (!StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }

        String destination = accessor.getDestination();
        Long userId = extractUserId(accessor.getUser());
        if (destination == null || userId == null) {
            throw new AccessDeniedException("구독 권한이 없습니다.");
        }

        for (String prefix : OWN_USER_ID_PREFIXES) {
            if (destination.startsWith(prefix)) {
                if (!userId.equals(parseLastSegment(destination))) {
                    throw new AccessDeniedException("구독 권한이 없습니다.");
                }
                return message;
            }
        }

        for (String prefix : CHAT_ROOM_PREFIXES) {
            if (destination.startsWith(prefix)) {
                if (!isParticipant(parseLastSegment(destination), userId)) {
                    throw new AccessDeniedException("구독 권한이 없습니다.");
                }
                return message;
            }
        }

        return message;
    }

    private boolean isParticipant(Long chatRoomId, Long userId) {
        if (chatRoomId == null) {
            return false;
        }
        ChatRoomDto chatRoom = chatRoomService.getChatRoom(chatRoomId);
        if (chatRoom == null) {
            return false;
        }
        ChatRequestDto chatRequest = chatRequestService.getChatRequest(chatRoom.getChatRequestId());
        if (chatRequest == null) {
            return false;
        }
        return userId.equals(chatRequest.getRequesterId()) || userId.equals(chatRequest.getResponderId());
    }

    private Long parseLastSegment(String destination) {
        String[] parts = destination.split("/");
        try {
            return Long.parseLong(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long extractUserId(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        return null;
    }
}
