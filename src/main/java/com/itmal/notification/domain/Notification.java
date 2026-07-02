package com.itmal.notification.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Notification {

    private Long notificationId;
    private String type;
    private String targetType;
    private Long targetId;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private Long userId;
    private String questionTitle;
    private Long questionId;
    private String answerContent;
    private String comment;
    private Boolean isDeleted;

}
