package com.itmal.notification.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NotificationResponseDto {
    private Long notificationId;
    private String targetType;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private String questionTitle;   //질문 제목 표시
    private Long questionId;  //targetId가 있지만 알림이 댓글 알림이라면 targetId는 댓글의 Id가 됨.
    // detail 페이지로 이동하려면 questionId가 필요하기에 따로 받는다.
    private String answerContent;   //답변 내용 표시
    private String comment; //댓글 내용 표시
}
