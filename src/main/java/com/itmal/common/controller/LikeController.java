package com.itmal.common.controller;

import com.itmal.answer.service.AnswerService;
import com.itmal.auth.domain.CustomUserDetails;
import com.itmal.common.dto.LikeResponseDto;
import com.itmal.common.dto.LikeTargetType;
import com.itmal.question.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class LikeController {

    private final AnswerService answerService;
    private final QuestionService questionService;

    @PostMapping("/{targetType}/{targetId}")
    public LikeResponseDto toggleLike(@PathVariable LikeTargetType targetType,
                                      @PathVariable Long targetId,
                                      @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();
        if (targetType == LikeTargetType.ANSWER) {
            AnswerLikeResponseDto result = answerService.toggleLike(targetId, userId);
            return new LikeResponseDto(result.isLiked(), result.getLikeCount());
        } else {
            QuestionLikeResponseDto result = questionService.toggleLike(targetId, userId);
            return new LikeResponseDto(result.isLiked(), result.getLikeCount());
        }
    }

}
