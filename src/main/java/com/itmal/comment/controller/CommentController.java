package com.itmal.comment.controller;

import com.itmal.auth.domain.CustomUserDetails;
import com.itmal.comment.dto.CommentRequestDto;
import com.itmal.comment.dto.CommentResponseDto;
import com.itmal.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/answers/{answerId}/comments")
    public CommentResponseDto createComment(@PathVariable Long answerId,
                                            @Valid @RequestBody CommentRequestDto requestDto,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return commentService.createComment(answerId, userDetails.getUserId(), requestDto);
    }


    @GetMapping("/answers/{answerId}/comments")
    public List<CommentResponseDto> getComment(@PathVariable Long answerId){
        return commentService.getComments(answerId);
    }


    @PutMapping("/comments/{commentId}")
    public CommentResponseDto updateComment(@PathVariable Long commentId,
                                            @Valid @RequestBody CommentRequestDto requestDto,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return commentService.updateComment(commentId, userDetails.getUserId(), requestDto);
    }

    @DeleteMapping("/comments/{commentId}")
    public int deleteComment(@PathVariable Long commentId,
                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        return commentService.deleteComment(commentId, userDetails.getUserId());
    }




}
