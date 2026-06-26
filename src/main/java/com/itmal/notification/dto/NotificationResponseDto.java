package com.itmal.notification.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NotificationResponseDto {
    private Long notificationId;
    private String targetType;
    private Long targetId;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private String message;
    private String questionTitle;
    private String comment;

}
