package com.itmal.question.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class QuestionAttachmentDto {
    private Long id;
    private String originalName;
    private String storedName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private LocalDateTime createdAt;
    private Long questionId;
}
