package com.itmal.tutorapplication.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TutorApplication {
    private Long tutorApplicationId;
    private Long userId;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;
    private LocalDateTime processedAt;
}
