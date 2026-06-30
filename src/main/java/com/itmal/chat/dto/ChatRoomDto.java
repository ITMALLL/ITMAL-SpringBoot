package com.itmal.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDto {
    //채팅방 pk 아이디(chatRoomId로 추후 변경)
    private Long id;
    @NotNull(message = "채팅 요청 ID는 필수입니다.")
    private Long chatRequestId;
    //채팅방 이름
    private String chatTitle;
    //채팅방 생성 일자
    private LocalDateTime createdAt;
    //채팅방 업데이트 일자
    private LocalDateTime updatedAt;
    //마지막 메세지 시각
    private LocalDateTime lastMessageAt;
    //A가 마지막 읽은 시각
    private LocalDateTime lastReadAtA;
    //B가 마지막 읽은 시각
    private LocalDateTime lastReadAtB;
    //A 나감 여부
    private Boolean hiddenByA;
    //B 나감 여부
    private Boolean hiddenByB;
}