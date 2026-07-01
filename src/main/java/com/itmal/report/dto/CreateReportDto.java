package com.itmal.report.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateReportDto {

    private Long targetId;
    private String targetType;
    private String reason;

}
