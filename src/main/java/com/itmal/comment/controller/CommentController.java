package com.itmal.comment.controller;

import com.itmal.comment.dto.CommentRequestDto;
import com.itmal.comment.dto.CommentResponseDto;
import com.itmal.comment.service.CommentService;
import com.itmal.global.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/answers/{answerId}/comments")
    public CommentResponseDto createComment(@PathVariable Long answerId,
                                            @Valid @RequestBody CommentRequestDto requestDto) {

        // TODO: 로그인 기능 후 다시 수정
        // 로그인 개발 후 Authentication/principal에서 실제 로그인한 유저의 userId를 꺼내서 교체할 것.
        Long userId = 1L;
        return commentService.createComment(answerId, userId, requestDto);
    }


    @GetMapping("/answers/{answerId}/comments")
    public List<CommentResponseDto> getComment(@PathVariable Long answerId){
        return commentService.getComments(answerId);
    }


    @PutMapping("/comments/{commentId}")
    public CommentResponseDto updateComment(@PathVariable Long commentId,
                                            @Valid @RequestBody CommentRequestDto requestDto){

        // TODO: 로그인 기능 후 다시 수정
        // 로그인 개발 후 Authentication/principal에서 실제 로그인한 유저의 userId를 꺼내서 교체할 것.
        Long userId = 1L;
        return commentService.updateComment(commentId, userId, requestDto);

    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationError(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<String> handleBusinessException(BusinessException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(e.getMessage());
    }

    @DeleteMapping("/comments/{commentId}")
    public int deleteComment(@PathVariable Long commentId){

        // TODO: 로그인 기능 후 다시 수정
        // 로그인 개발 후 Authentication/principal에서 실제 로그인한 유저의 userId를 꺼내서 교체할 것.
        Long userId = 1L;
        return commentService.deleteComment(commentId, userId);
    }




}
