package com.itmal.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    //기본 메세지 pk 아이디
    private Long chatMessageId;

    @NotNull(message = "채팅방 ID는 필수입니다.")
    private Long chatRoomId;

    @NotNull(message = "보내는 사람 ID는 필수입니다.")
    private Long senderId;

    @NotBlank(message = "메시지 내용은 필수입니다.")
    private String content;

    //읽었는가
    private Boolean isRead;
    //언제 보냈는지
    private LocalDateTime createdAt;
}