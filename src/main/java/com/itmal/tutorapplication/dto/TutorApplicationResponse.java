package com.itmal.tutorapplication.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TutorApplicationResponse {
    private Long tutorApplicationId;
    private Long userId;
    private String nickname;
    private String email;
    private LocalDateTime appliedAt;
}
