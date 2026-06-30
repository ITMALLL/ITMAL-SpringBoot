package com.itmal.chat.mapper;

import com.itmal.chat.dto.ChatRoomDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

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

    // isRequester 플래그를 받아서 처리
    void leaveRoom(@Param("chatRoomId") Long chatRoomId,
                   @Param("isRequester") Boolean isRequester);

    // A가 복구 (B가 메시지 보냈을 때)
    void restoreHiddenA(@Param("id") Long id);

    // B가 복구 (A가 메시지 보냈을 때)
    void restoreHiddenB(@Param("id") Long id);

    // 사용자의 모든 채팅방 조회 (hidden_by 고려)
    List<ChatRoomDto> selectByUserId(@Param("userId") Long userId);
}