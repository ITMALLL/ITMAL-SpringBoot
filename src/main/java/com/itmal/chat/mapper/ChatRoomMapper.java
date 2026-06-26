package com.itmal.chat.mapper;

import com.itmal.chat.dto.ChatRoomDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;

@Mapper
public interface ChatRoomMapper {

    // 채팅방 생성
    void insertChatRoom(ChatRoomDto chatRoom);

    // ID로 조회
    ChatRoomDto selectById(@Param("id") Long id);

    // chat_request_id로 조회
    ChatRoomDto selectByChatRequestId(@Param("chatRequestId") Long chatRequestId);

    // 마지막 메시지 시간 업데이트
    void updateLastMessageAt(@Param("id") Long id,
                             @Param("lastMessageAt") LocalDateTime lastMessageAt);

    // last_read_at 업데이트 (사용자 A)
    void updateLastReadAtA(@Param("id") Long id,
                           @Param("lastReadAtA") LocalDateTime lastReadAtA);

    // last_read_at 업데이트 (사용자 B)
    void updateLastReadAtB(@Param("id") Long id,
                           @Param("lastReadAtB") LocalDateTime lastReadAtB);

    // 사용자가 채팅방 나감 (현재 사용자 ID를 받아서 A인지 B인지 판단)
    void leaveRoom(@Param("chatRoomId") Long chatRoomId,
                   @Param("userId") Long userId,
                   @Param("chatRequestId") Long chatRequestId);

    // A가 복구 (B가 메시지 보냈을 때)
    void restoreHiddenA(@Param("id") Long id);

    // B가 복구 (A가 메시지 보냈을 때)
    void restoreHiddenB(@Param("id") Long id);

}