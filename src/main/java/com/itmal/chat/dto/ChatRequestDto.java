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
public class ChatRequestDto {
    //기본 채팅 요청 pk 아이디
    private Long chatRequestId;
    private Long requesterId;

    @NotNull(message = "응답자 ID는 필수입니다.")
    private Long responderId;

    @NotBlank(message = "인트로 메시지는 필수입니다.")
    private String introMessage;
    //현재 상태(대기, 수락, 거절)
    private String status;  // PENDING, ACCEPTED, REJECTED
    //요청 생성 시각
    private LocalDateTime createdAt;
    //요청 응답 시각
    private LocalDateTime respondedAt;
}
