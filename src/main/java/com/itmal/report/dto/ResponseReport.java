package com.itmal.report.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ResponseReport {
    private Long reportId;
    private String targetType;
    private Long targetId;
    private String reason;
    private String status;
    private LocalDateTime createdAt;

    private String reporterNickname;
    private Long questionId;
    private Boolean isTargetDeleted;
}
